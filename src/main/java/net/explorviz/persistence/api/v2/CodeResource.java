package net.explorviz.persistence.api.v2;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.explorviz.persistence.api.v2.model.BranchDto;
import net.explorviz.persistence.api.v2.model.BranchPointDto;
import net.explorviz.persistence.api.v2.model.CommitTreeDto;
import net.explorviz.persistence.api.v2.model.landscape.ApplicationDto;
import net.explorviz.persistence.api.v2.model.landscape.LandscapeDto;
import net.explorviz.persistence.api.v2.model.landscape.NodeDto;
import net.explorviz.persistence.api.v2.model.metrics.ApplicationMetricsCodeDto;
import net.explorviz.persistence.api.v2.model.metrics.ClassMetricCodeDto;
import net.explorviz.persistence.api.v2.model.metrics.FileMetricCodeDto;
import net.explorviz.persistence.api.v2.model.metrics.MethodMetricCodeDto;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Clazz;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.ClazzRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.FunctionRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/v2/code")
@SuppressWarnings("PMD.UseObjectForClearerAPI")
class CodeResource {

  /**
   * Dummy branch point expected by frontend if no branch point exists (e.g. for the main branch).
   */
  private static final BranchPointDto NO_BRANCH_POINT = new BranchPointDto("NONE", "");

  @Inject
  private ApplicationRepository applicationRepository;

  @Inject
  private ClazzRepository clazzRepository;

  @Inject
  private CommitRepository commitRepository;

  @Inject
  private FileRevisionRepository fileRevisionRepository;

  @Inject
  private FunctionRepository functionRepository;

  @Inject
  private SessionFactory sessionFactory;

  @GET
  @Path("/applications/{landscapeToken}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> getStaticApplicationNamesForLandscape(@RestPath final String landscapeToken) {
    final Session session = sessionFactory.openSession();
    // TODO return all applications instead of only those with static data?
    return applicationRepository.findStaticApplicationNamesForLandscapeToken(session,
        landscapeToken);
  }

  @GET
  @Path("/commit-tree/{landscapeToken}/{applicationName}")
  @Produces(MediaType.APPLICATION_JSON)
  public CommitTreeDto getCommitTreeForApplicationAndLandscape(
      @RestPath final String landscapeToken, @RestPath final String applicationName) {
    final Session session = sessionFactory.openSession();

    if (applicationRepository.findApplicationByNameAndLandscapeToken(session, applicationName,
        landscapeToken).isEmpty()) {
      throw new NotFoundException("The requested application does not exist in the database.");
    }

    final List<Commit> commits =
        commitRepository.findCommitsWithBranchForApplicationAndLandscapeToken(session,
            landscapeToken, applicationName);

    final Map<String, ArrayList<String>> commitsMap = new HashMap<>();
    final Map<String, BranchPointDto> branchPointMap = new HashMap<>();

    for (final Commit commit : commits) {
      final String branchName = commit.getBranch().getName();

      commitsMap.computeIfAbsent(branchName, k -> new ArrayList<>()).add(commit.getHash());

      final Set<Commit> parentCommits = commit.getParentCommits();
      if (parentCommits.isEmpty()) {
        branchPointMap.putIfAbsent(branchName, NO_BRANCH_POINT);
        continue;
      }

      // If all parent commits are assigned to a different branch than the current commit, then we
      // treat this as the first commit unique to this branch and therefore create a branch point
      // from the first of the parent commits. Usually, there is only 1 parent commit in this case.
      parentCommits.stream().filter(pc -> !branchName.equals(pc.getBranch().getName())).findFirst()
          .ifPresent(parentCommit -> branchPointMap.putIfAbsent(branchName,
              new BranchPointDto(parentCommit.getHash(), parentCommit.getBranch().getName())));
    }

    final List<BranchDto> branches = commitsMap.entrySet().stream()
        .map(e -> new BranchDto(e.getKey(), e.getValue(), branchPointMap.get(e.getKey()))).toList();

    return new CommitTreeDto(applicationName, branches);
  }

  @GET
  @Path("/metrics/{landscapeToken}/{applicationName}/{commitHash}")
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationMetricsCodeDto getStaticCodeMetricsForApplicationAndCommit(
      @RestPath final String landscapeToken, @RestPath final String applicationName,
      @RestPath final String commitHash) {
    final Session session = sessionFactory.openSession();

    final Map<String, FileRevision> appFiles =
        fileRevisionRepository.findStaticFilesWithFqnForApplicationAndCommitAndLandscapeToken(
            session, applicationName, commitHash, landscapeToken);

    final Map<String, Clazz> appClasses =
        clazzRepository.findStaticClassesWithFqnForApplicationAndCommitAndLandscapeToken(session,
            applicationName, commitHash, landscapeToken);

    final Map<String, Function> appFunctions =
        functionRepository.findStaticFunctionsWithFqnForApplicationAndCommitAndLandscapeToken(
            session, applicationName, commitHash, landscapeToken);

    final Map<String, FileMetricCodeDto> fileMetricMap = appFiles.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> new FileMetricCodeDto(e.getValue())));

    final Map<String, ClassMetricCodeDto> classMetricMap = appClasses.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> new ClassMetricCodeDto(e.getValue())));

    final Map<String, MethodMetricCodeDto> methodMetricMap = appFunctions.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> new MethodMetricCodeDto(e.getValue())));

    return new ApplicationMetricsCodeDto(fileMetricMap, classMetricMap, methodMetricMap);
  }

  @GET
  @Path("/structure/{landscapeToken}/{applicationName}/{commitHash1}-{commitHash2}")
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeDto getStaticStructureForApplicationAndTwoCommits(
      @RestPath final String landscapeToken, @RestPath final String applicationName,
      @RestPath final String commitHash1, @RestPath final String commitHash2) {
    final Session session = sessionFactory.openSession();
    final Optional<Application> applicationsForCommitsOptional =
        applicationRepository.fetchApplicationHydratedForTwoCommits(session, applicationName,
            commitHash1, commitHash2, landscapeToken);
    final List<ApplicationDto> applicationDtoList =
        applicationsForCommitsOptional.map(a -> List.of(new ApplicationDto(a))).orElse(List.of());
    final NodeDto node = new NodeDto("", "", applicationDtoList);
    return new LandscapeDto(landscapeToken, List.of(node), List.of());
  }

  @GET
  @Path("/structure/{landscapeToken}/{applicationName}/{commitHash}")
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeDto getStaticStructureForApplicationAndSingleCommit(
      @RestPath final String landscapeToken, @RestPath final String applicationName,
      @RestPath final String commitHash) {
    // Re-use query for two-commit case by supplying same hash twice
    return getStaticStructureForApplicationAndTwoCommits(landscapeToken, applicationName,
        commitHash, commitHash);
  }
}
