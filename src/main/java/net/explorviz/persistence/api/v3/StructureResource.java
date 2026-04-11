package net.explorviz.persistence.api.v3;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.explorviz.persistence.api.v3.model.CommitComparison;
import net.explorviz.persistence.api.v3.model.TypeOfAnalysis;
import net.explorviz.persistence.api.v3.model.conversion.CommitComparisonApplicationToCityConverter;
import net.explorviz.persistence.api.v3.model.conversion.DefaultApplicationToCityConverter;
import net.explorviz.persistence.api.v3.model.conversion.LandscapeFlattener;
import net.explorviz.persistence.api.v3.model.landscape.CityDto.CityConvertible;
import net.explorviz.persistence.api.v3.model.landscape.FlatLandscapeDto;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.CommitRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

/** Contains endpoints concerning landscape structure data, i.e. the shape of the landscape. */
@Path("/v3/landscapes/{landscapeToken}/structure")
public class StructureResource {

  @Inject SessionFactory sessionFactory;

  @Inject ApplicationRepository applicationRepository;

  @Inject CommitRepository commitRepository;

  /** Retrieve all structure data gathered from runtime analysis. */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/runtime")
  public FlatLandscapeDto getRuntimeStructureData(@RestPath final String landscapeToken) {
    final Session session = sessionFactory.openSession();

    final List<Application> ogmApps =
        applicationRepository.fetchAllApplicationsHydratedForRuntimeData(session, landscapeToken);

    return LandscapeFlattener.flattenLandscape(
        landscapeToken,
        ogmApps.stream()
            .map(a -> DefaultApplicationToCityConverter.convert(a, TypeOfAnalysis.RUNTIME))
            .toList());
  }

  /**
   * Retrieve structure data gathered from static analysis for a particular application and commit.
   *
   * @param landscapeToken String identifier of the landscape
   * @param repositoryName Name of the repository for which to retrieve structure data
   * @param commitHash Identifier of the git commit for which to retrieve structure
   * @return The flat landscape containing the applications of the repository at the given commit,
   *     where each application represents a city
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/evolution/{repositoryName}/{commitHash}")
  public FlatLandscapeDto getStaticStructureData(
      @RestPath final String landscapeToken,
      @RestPath final String repositoryName,
      @RestPath final String commitHash) {
    final Session session = sessionFactory.openSession();

    final List<Application> ogmApps =
        applicationRepository.fetchHydratedApplicationsInRepositoryForCommit(
            session, landscapeToken, repositoryName, commitHash);

    return LandscapeFlattener.flattenLandscape(
        landscapeToken,
        ogmApps.stream()
            .map(a -> DefaultApplicationToCityConverter.convert(a, TypeOfAnalysis.STATIC))
            .toList());
  }

  /**
   * Retrieve union of structure data for the two provided commits within the given repository. The
   * {@link CommitComparison} value is set relative to the second commit, e.g. "DELETED" is written
   * if some component is present in the first commit, but not the second.
   *
   * @param landscapeToken String identifier of the landscape
   * @param repositoryName Name of the repository for which to retrieve structure data
   * @param firstCommitHash Identifier of the first git commit against which to compare the second
   * @param secondCommitHash Identifier of the second git commit for which to retrieve structure.
   *     All {@link CommitComparison} values are given relative to this commit
   * @return A flat landscape containing the union of the applications in the specified repository
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/evolution/{repositoryName}/{firstCommitHash}-{secondCommitHash}")
  public FlatLandscapeDto getCombinedStaticStructureData(
      @RestPath final String landscapeToken,
      @RestPath final String repositoryName,
      @RestPath final String firstCommitHash,
      @RestPath final String secondCommitHash) {
    final Session session = sessionFactory.openSession();

    final List<Application> firstCommitApps =
        applicationRepository.fetchHydratedApplicationsInRepositoryForCommit(
            session, landscapeToken, repositoryName, firstCommitHash);

    final List<Application> secondCommitApps =
        applicationRepository.fetchHydratedApplicationsInRepositoryForCommit(
            session, landscapeToken, repositoryName, secondCommitHash);

    final List<CityConvertible> cityConvertibles = new ArrayList<>();

    firstCommitApps.forEach(
        firstCommitApp -> {
          final Optional<Application> secondApp =
              secondCommitApps.stream()
                  .filter(a -> a.getName().equals(firstCommitApp.getName()))
                  .findAny();

          cityConvertibles.add(
              secondApp
                  .map(
                      secondCommitApp ->
                          CommitComparisonApplicationToCityConverter.convert(
                              firstCommitApp, secondCommitApp))
                  .orElseGet(() -> DefaultApplicationToCityConverter.convert(firstCommitApp)));
        });

    return LandscapeFlattener.flattenLandscape(landscapeToken, cityConvertibles);
  }
}
