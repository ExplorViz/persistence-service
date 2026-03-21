package net.explorviz.persistence.api.v2;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.explorviz.persistence.api.v2.model.BranchDto;
import net.explorviz.persistence.api.v2.model.BranchPointDto;
import net.explorviz.persistence.api.v2.model.CommitComparisonDto;
import net.explorviz.persistence.api.v2.model.CommitTreeDto;
import net.explorviz.persistence.api.v2.model.EntityMetricsComparison;
import net.explorviz.persistence.api.v2.model.EntityMetricsComparison.ValueComparison;
import net.explorviz.persistence.api.v2.model.TimestampDto;
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
import net.explorviz.persistence.repository.CommitRepository.FileComparison;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.FunctionRepository;
import net.explorviz.persistence.repository.TraceRepository;
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

  @Inject TraceRepository traceRepository;

  @Inject private ApplicationRepository applicationRepository;

  @Inject private ClazzRepository clazzRepository;

  @Inject private CommitRepository commitRepository;

  @Inject private FileRevisionRepository fileRevisionRepository;

  @Inject private FunctionRepository functionRepository;

  @Inject private SessionFactory sessionFactory;

  @GET
  @Path("/applications/{landscapeToken}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> getStaticApplicationNamesForLandscape(@RestPath final String landscapeToken) {
    final Session session = sessionFactory.openSession();
    // TODO return all applications instead of only those with static data?
    return applicationRepository.findStaticApplicationNamesForLandscapeToken(
        session, landscapeToken);
  }

  @GET
  @Path("/commit-tree/{landscapeToken}/{applicationName}")
  @Produces(MediaType.APPLICATION_JSON)
  public CommitTreeDto getCommitTreeForApplicationAndLandscape(
      @RestPath final String landscapeToken, @RestPath final String applicationName) {
    final Session session = sessionFactory.openSession();

    if (applicationRepository
        .findApplicationByNameAndLandscapeToken(session, applicationName, landscapeToken)
        .isEmpty()) {
      throw new NotFoundException("The requested application does not exist in the database.");
    }

    final List<Commit> commits =
        commitRepository.findCommitsWithBranchForApplicationAndLandscapeToken(
            session, landscapeToken, applicationName);

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
      parentCommits.stream()
          .filter(pc -> !branchName.equals(pc.getBranch().getName()))
          .findFirst()
          .ifPresent(
              parentCommit ->
                  branchPointMap.putIfAbsent(
                      branchName,
                      new BranchPointDto(
                          parentCommit.getHash(), parentCommit.getBranch().getName())));
    }

    final List<BranchDto> branches =
        commitsMap.entrySet().stream()
            .map(e -> new BranchDto(e.getKey(), e.getValue(), branchPointMap.get(e.getKey())))
            .toList();

    return new CommitTreeDto(applicationName, branches);
  }

  @GET
  @Path("/metrics/{landscapeToken}/{applicationName}/{commitHash}")
  @Produces(MediaType.APPLICATION_JSON)
  public ApplicationMetricsCodeDto getStaticCodeMetricsForApplicationAndCommit(
      @RestPath final String landscapeToken,
      @RestPath final String applicationName,
      @RestPath final String commitHash) {
    final Session session = sessionFactory.openSession();

    final Map<String, FileRevision> appFiles =
        fileRevisionRepository.findStaticFilesWithFqnForApplicationAndCommitAndLandscapeToken(
            session, applicationName, commitHash, landscapeToken);

    final Map<String, Clazz> appClasses =
        clazzRepository.findStaticClassesWithFqnForApplicationAndCommitAndLandscapeToken(
            session, applicationName, commitHash, landscapeToken);

    final Map<String, Function> appFunctions =
        functionRepository.findStaticFunctionsWithFqnForApplicationAndCommitAndLandscapeToken(
            session, applicationName, commitHash, landscapeToken);

    final Map<String, FileMetricCodeDto> fileMetricMap =
        appFiles.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> new FileMetricCodeDto(e.getValue())));

    final Map<String, ClassMetricCodeDto> classMetricMap =
        appClasses.entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, e -> new ClassMetricCodeDto(e.getValue())));

    final Map<String, MethodMetricCodeDto> methodMetricMap =
        appFunctions.entrySet().stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, e -> new MethodMetricCodeDto(e.getValue())));

    return new ApplicationMetricsCodeDto(fileMetricMap, classMetricMap, methodMetricMap);
  }

  @GET
  @Path("/structure/{landscapeToken}/{applicationName}/{firstCommitHash}-{secondCommitHash}")
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeDto getStaticStructureForApplicationAndTwoCommits(
      @RestPath final String landscapeToken,
      @RestPath final String applicationName,
      @RestPath final String firstCommitHash,
      @RestPath final String secondCommitHash) {
    final Session session = sessionFactory.openSession();

    final List<Application> applications =
        applicationRepository.fetchApplicationsHydratedForTwoCommits(
            session, landscapeToken, firstCommitHash, secondCommitHash);

    final List<ApplicationDto> applicationDtoList =
        applications.stream()
            .filter(a -> applicationName.equals(a.getName()))
            .map(ApplicationDto::new)
            .toList();

    final NodeDto node = new NodeDto("", "", applicationDtoList);
    return new LandscapeDto(landscapeToken, List.of(node), List.of());
  }

  @GET
  @Path("landscapes/{landscapeToken}/timestamps")
  @Produces(MediaType.APPLICATION_JSON)
  public Multi<TimestampDto> getTimestamps(
      @RestPath final String landscapeToken,
      @QueryParam("newest") Long newest,
      @QueryParam("oldest") Long oldest,
      @QueryParam("commit") final String commit,
      @QueryParam("bucketSize") Long bucketSize) {
    final Session session = sessionFactory.openSession();

    final List<TimestampDto> timestamps;

    if (newest == null) {
      newest = Long.MAX_VALUE;
    }

    if (oldest == null) {
      oldest = 0L;
    }

    if (bucketSize == null || bucketSize <= 0) {
      bucketSize = 1000000000L;
    }

    if (commit != null) {
      timestamps =
          traceRepository.findTimestampsForLandscapeTokenCommitAndTimeRange(
              session, landscapeToken, newest, oldest, commit, bucketSize);
    } else {
      timestamps =
          traceRepository.findTimestampsForLandscapeTokenCommitAndTimeRange(
              session, landscapeToken, newest, oldest, bucketSize);
    }

    return Multi.createFrom().iterable(timestamps);
  }

  @DELETE
  @Path("landscapes/{landscapeToken}/trace-data")
  @Produces(MediaType.APPLICATION_JSON)
  public void deleteTraceData(@RestPath final String landscapeToken) {
    final Session session = sessionFactory.openSession();

    traceRepository.deleteTraceData(session, landscapeToken);
  }

  @GET
  @Path("/structure/{landscapeToken}/{applicationName}/{commitHash}")
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeDto getStaticStructureForApplicationAndSingleCommit(
      @RestPath final String landscapeToken,
      @RestPath final String applicationName,
      @RestPath final String commitHash) {
    // Re-use query for two-commit case by supplying same hash twice
    return getStaticStructureForApplicationAndTwoCommits(
        landscapeToken, applicationName, commitHash, commitHash);
  }

  @GET
  @Path(
      "/commit-comparison/{landscapeToken}/{applicationName}/{firstCommitHash}-{secondCommitHash}")
  @Produces(MediaType.APPLICATION_JSON)
  public CommitComparison getCommitComparison(
      @RestPath final String landscapeToken,
      @RestPath final String applicationName,
      @RestPath final String firstCommitHash,
      @RestPath final String secondCommitHash) {
    final Session session = sessionFactory.openSession();

    final List<String> modifiedFiles =
        commitRepository.findModifiedFileFqns(
            session, landscapeToken, applicationName, firstCommitHash, secondCommitHash);

    final List<String> addedFiles =
        commitRepository.findAddedFileFqns(
            session, landscapeToken, applicationName, firstCommitHash, secondCommitHash);

    final List<String> deletedFiles =
        commitRepository.findDeletedFileFqns(
            session, landscapeToken, applicationName, firstCommitHash, secondCommitHash);

    final List<String> addedPackages =
        commitRepository.findAddedDirectoryFqns(
            session, landscapeToken, applicationName, firstCommitHash, secondCommitHash);

    final List<String> deletedPackages =
        commitRepository.findDeletedDirectoryFqns(
            session, landscapeToken, applicationName, firstCommitHash, secondCommitHash);

    final List<FileComparison> metricResults =
        commitRepository.findFilesWithCounterpart(
            session, landscapeToken, applicationName, firstCommitHash, secondCommitHash);

    final List<EntityMetricsComparison> entityMetricsComparisons =
        metricResults.stream()
            .map(
                result -> {
                  final Map<String, Double> newMetrics = result.fileFirstCommit().getMetrics();
                  final Map<String, Double> oldMetrics =
                      (result.fileSecondCommit() != null)
                          ? result.fileSecondCommit().getMetrics()
                          : Map.of();

                  final Map<String, ValueComparison> metricComparisons =
                      newMetrics.entrySet().stream()
                          .collect(
                              Collectors.toMap(
                                  Map.Entry::getKey,
                                  entry -> {
                                    final String oldValue =
                                        Objects.toString(oldMetrics.get(entry.getKey()), null);
                                    final String newValue = entry.getValue().toString();
                                    return new ValueComparison(oldValue, newValue);
                                  }));

                  return new EntityMetricsComparison(result.fileFqn(), metricComparisons);
                })
            .toList();

          return new EntityMetricsComparison(result.fileFqn(), metricComparisons);
        }).toList();

    return new CommitComparisonDto(modifiedFiles, addedFiles, deletedFiles, addedPackages,
        deletedPackages, entityMetricsComparisons);
  }
}
