package net.explorviz.persistence.api.v2;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.explorviz.persistence.api.v2.model.TimestampDto;
import net.explorviz.persistence.api.v2.model.TraceDto;
import net.explorviz.persistence.api.v2.model.landscape.ApplicationDto;
import net.explorviz.persistence.api.v2.model.landscape.LandscapeDto;
import net.explorviz.persistence.api.v2.model.landscape.NodeDto;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Trace;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.RepositoryRepository;
import net.explorviz.persistence.repository.TraceRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/v2/landscapes/{landscapeToken}")
@SuppressWarnings("PMD.UseObjectForClearerAPI")
class LandscapeResource {

  @Inject private SessionFactory sessionFactory;

  @Inject private ApplicationRepository applicationRepository;

  @Inject private CommitRepository commitRepository;

  @Inject private RepositoryRepository repositoryRepository;

  @Inject private TraceRepository traceRepository;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/structure")
  public LandscapeDto getStructureData(@RestPath final String landscapeToken) {
    final Session session = sessionFactory.openSession();

    final List<Application> ogmApps =
        applicationRepository.fetchAllApplicationsHydratedForRuntimeData(session, landscapeToken);

    final NodeDto node = new NodeDto("", "", ogmApps.stream().map(ApplicationDto::new).toList());
    return new LandscapeDto(landscapeToken, List.of(node), List.of());
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/dynamic")
  public List<TraceDto> getDynamicData(
      @RestPath final String landscapeToken, @RestQuery final Long from, @RestQuery final Long to) {

    final Session session = sessionFactory.openSession();

    final long fromTimestamp = Objects.requireNonNullElse(from, Long.MIN_VALUE);
    final long toTimestamp = Objects.requireNonNullElse(to, Long.MAX_VALUE);

    final List<Trace> ogmTraces =
        traceRepository.findHydratedTraces(session, landscapeToken, fromTimestamp, toTimestamp);

    return ogmTraces.stream()
        .map(
            t -> {
              final Optional<String> commitHash =
                  traceRepository.findCommitHashForTrace(session, landscapeToken, t.getTraceId());
              return new TraceDto(t, landscapeToken, commitHash.orElse("UNKNOWN"));
            })
        .toList();
  }

  @GET
  @Path("/timestamps")
  @Produces(MediaType.APPLICATION_JSON)
  public Multi<TimestampDto> getTimestamps(
      @RestPath final String landscapeToken,
      @RestQuery final Long newest,
      @RestQuery final Long oldest,
      @RestQuery final String commit) {
    final Session session = sessionFactory.openSession();

    final List<TimestampDto> timestamps;
    Long newestTimestamp = newest;
    if (newestTimestamp == null) {
      newestTimestamp = Long.MAX_VALUE;
    }
    Long oldestTimestamp = oldest;
    if (oldestTimestamp == null) {
      oldestTimestamp = 0L;
    }

    if (commit != null) {
      timestamps =
          traceRepository.findTimestampsForLandscapeTokenCommitAndTimeRange(
              session, landscapeToken, newestTimestamp, oldestTimestamp, commit, 1_000_000_000L);
    } else {
      timestamps =
          traceRepository.findTimestampsForLandscapeTokenCommitAndTimeRange(
              session, landscapeToken, newestTimestamp, oldestTimestamp, 1_000_000_000L);
    }

    return Multi.createFrom().iterable(timestamps);
  }
}
