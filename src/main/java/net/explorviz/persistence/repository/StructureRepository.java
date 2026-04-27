package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.explorviz.persistence.api.v3.model.CommitComparison;
import net.explorviz.persistence.api.v3.model.MetricValue;
import net.explorviz.persistence.api.v3.model.TypeOfAnalysis;
import net.explorviz.persistence.api.v3.model.landscape.BuildingDto;
import net.explorviz.persistence.api.v3.model.landscape.CityDto;
import net.explorviz.persistence.api.v3.model.landscape.DistrictDto;
import net.explorviz.persistence.api.v3.model.landscape.FlatBaseModel;
import net.explorviz.persistence.api.v3.model.landscape.FlatLandscapeDto;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;

@ApplicationScoped
public class StructureRepository {

  @Inject
  StructureMapper mapper;

  public record StaticDataRequest(
      String landscapeToken,
      String repositoryName,
      String commitHash
  ) {}

  public record CombinedStaticDataRequest(
      String landscapeToken,
      String repositoryName,
      String firstCommitHash,
      String secondCommitHash
  ) {}

  public FlatLandscapeDto fetchFlatLandscapeForRuntimeData(
      final Session session, final String landscapeToken) {
    final String query =
        """
        MATCH (l:Landscape {tokenId: $tokenId})
        MATCH (func:Function)
        WHERE (l)-[:CONTAINS]->(:Trace)-[:CONTAINS]->(:Span)-[:REPRESENTS]->(func)

        MATCH p = (a:Application)-[:HAS_ROOT]->(root:Directory)-[:CONTAINS*0..]->(func)
        WHERE (l)-[:CONTAINS]->(a)

        WITH DISTINCT a, nodes(p) AS pathNodes

        UNWIND [a] + pathNodes AS n
        WITH DISTINCT n, a
        RETURN
          id(n) AS id,
          labels(n) AS labels,
          properties(n) AS properties,
          id(a) AS cityId,
          [(n)-[:HAS_ROOT|CONTAINS]->(m) | id(m)] AS childrenIds,
          [(n)<-[:HAS_ROOT|CONTAINS]-(p) | id(p)][0] AS parentId
        """;

    final Result result = session.query(query, Map.of("tokenId", landscapeToken));
    return mapper.buildFlatLandscape(landscapeToken, result, TypeOfAnalysis.RUNTIME);
  }

  public FlatLandscapeDto fetchFlatLandscapeForStaticData(
      final Session session,
      final StaticDataRequest request) {
    final String query =
        """
        MATCH (l:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(:Repository {name: $repoName})
          -[:CONTAINS]->(:Commit {hash: $commitHash})
        MATCH (c:Commit {hash: $commitHash})-[:CONTAINS]->(f:FileRevision)

        MATCH p = (a:Application)-[:HAS_ROOT]->(root:Directory)-[:CONTAINS*0..]->(f)
        WHERE (l)-[:CONTAINS]->(a)

        WITH DISTINCT a, nodes(p) AS pathNodes

        UNWIND [a] + pathNodes AS n
        WITH DISTINCT n, a
        RETURN
          id(n) AS id,
          labels(n) AS labels,
          properties(n) AS properties,
          id(a) AS cityId,
          [(n)-[:HAS_ROOT|CONTAINS]->(m) | id(m)] AS childrenIds,
          [(n)<-[:HAS_ROOT|CONTAINS]-(p) | id(p)][0] AS parentId
        """;

    final Result result =
        session.query(
            query,
            Map.of("tokenId", request.landscapeToken(), "repoName", request.repositoryName(),
                "commitHash", request.commitHash()));
    return mapper.buildFlatLandscape(request.landscapeToken(), result, TypeOfAnalysis.STATIC);
  }

  public FlatLandscapeDto fetchCombinedFlatLandscape(
      final Session session,
      final CombinedStaticDataRequest request) {

    final FlatLandscapeDto first = fetchFlatLandscapeForStaticData(session,
        new StaticDataRequest(request.landscapeToken(), request.repositoryName(),
            request.firstCommitHash()));
    final FlatLandscapeDto second = fetchFlatLandscapeForStaticData(session,
        new StaticDataRequest(request.landscapeToken(), request.repositoryName(),
            request.secondCommitHash()));

    return merge(request.landscapeToken(), first, second);
  }

  private FlatLandscapeDto merge(
      final String token, final FlatLandscapeDto first, final FlatLandscapeDto second) {
    final Map<String, CityDto> cities = new HashMap<>();
    final Map<String, DistrictDto> districts = new HashMap<>();
    final Map<String, BuildingDto> buildings = new HashMap<>();

    mergeNodes(first.cities(), second.cities(), cities, CommitComparison.ADDED,
        CommitComparison.REMOVED);
    mergeNodes(first.districts(), second.districts(), districts, CommitComparison.ADDED,
        CommitComparison.REMOVED);
    mergeNodes(first.buildings(), second.buildings(), buildings, CommitComparison.ADDED,
        CommitComparison.REMOVED);

    return new FlatLandscapeDto(token, cities, districts, buildings);
  }

  private <T> void mergeNodes(
      final Map<String, T> firstMap,
      final Map<String, T> secondMap,
      final Map<String, T> targetMap,
      final CommitComparison added,
      final CommitComparison removed) {

    final Map<String, T> firstByFqn = firstMap.values().stream()
        .collect(Collectors.toMap(this::getFqn, java.util.function.Function.identity()));
    final Map<String, T> secondByFqn = secondMap.values().stream()
        .collect(Collectors.toMap(this::getFqn, java.util.function.Function.identity()));

    final Set<String> allFqns =
        Stream.concat(firstByFqn.keySet().stream(), secondByFqn.keySet().stream())
            .collect(Collectors.toSet());

    for (final String fqn : allFqns) {
      final T firstNode = firstByFqn.get(fqn);
      final T secondNode = secondByFqn.get(fqn);

      if (firstNode != null && secondNode != null) {
        if (getId(firstNode).equals(getId(secondNode))) {
          targetMap.put(getId(secondNode),
              withComparison(secondNode, CommitComparison.UNCHANGED, firstNode));
        } else {
          targetMap.put(getId(secondNode),
              withComparison(secondNode, CommitComparison.MODIFIED, firstNode));
        }
      } else if (firstNode != null) {
        targetMap.put(getId(firstNode), withComparison(firstNode, removed, null));
      } else {
        targetMap.put(getId(secondNode), withComparison(secondNode, added, null));
      }
    }
  }

  private String getFqn(final Object dto) {
    if (dto instanceof CityDto d) {
      return d.flatBaseModel().name();
    }
    if (dto instanceof DistrictDto d) {
      return d.flatBaseModel().fqn();
    }
    if (dto instanceof BuildingDto d) {
      return d.flatBaseModel().fqn();
    }
    return "";
  }

  private String getId(final Object dto) {
    if (dto instanceof CityDto d) {
      return d.flatBaseModel().id();
    }
    if (dto instanceof DistrictDto d) {
      return d.flatBaseModel().id();
    }
    if (dto instanceof BuildingDto d) {
      return d.flatBaseModel().id();
    }
    return "";
  }

  @SuppressWarnings("unchecked")
  private <T> T withComparison(final T dto, final CommitComparison comp, final T previousDto) {
    if (dto instanceof CityDto d) {
      return (T) new CityDto(withBaseComparison(d.flatBaseModel(), comp), d.districtIds(),
          d.buildingIds(), d.allContainedDistrictIds(), d.allContainedBuildingIds());
    }
    if (dto instanceof DistrictDto d) {
      return (T) new DistrictDto(withBaseComparison(d.flatBaseModel(), comp), d.parentCityId(),
          d.parentDistrictId(), d.districtIds(), d.buildingIds());
    }
    if (dto instanceof BuildingDto d) {
      final Map<String, MetricValue> metrics = previousDto instanceof BuildingDto p 
          ? mergeMetrics(d.metrics(), p.metrics()) 
          : d.metrics();
      return (T) new BuildingDto(withBaseComparison(d.flatBaseModel(), comp), d.parentCityId(),
          d.parentDistrictId(), d.language(), metrics);
    }
    return dto;
  }

  private FlatBaseModel withBaseComparison(final FlatBaseModel base, final CommitComparison comp) {
    return new FlatBaseModel(base.id(), base.name(), base.fqn(), base.originOfData(), comp);
  }

  private Map<String, MetricValue> mergeMetrics(final Map<String, MetricValue> current,
      final Map<String, MetricValue> previous) {
    final Map<String, MetricValue> merged = new HashMap<>();
    current.forEach((k, v) -> {
      final MetricValue prev = previous.get(k);
      merged.put(k, new MetricValue(v.current(), prev != null ? prev.current() : null));
    });
    return merged;
  }
}
