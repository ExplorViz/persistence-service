package net.explorviz.persistence.api.v3;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import net.explorviz.persistence.api.v3.model.conversion.DefaultApplicationToCityConverter;
import net.explorviz.persistence.api.v3.model.conversion.LandscapeFlattener;
import net.explorviz.persistence.api.v3.model.landscape.FlatLandscapeDto;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.CommitRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/v3/landscapes/{landscapeToken}")
class LandscapeResource {

  @Inject SessionFactory sessionFactory;

  @Inject ApplicationRepository applicationRepository;

  @Inject CommitRepository commitRepository;

  /** Retrieve structure data gathered only from runtime analysis. */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/structure/runtime")
  public FlatLandscapeDto getRuntimeStructureData(@RestPath final String landscapeToken) {
    final Session session = sessionFactory.openSession();

    final List<Application> ogmApps =
        applicationRepository.fetchAllApplicationsHydratedForRuntimeData(session, landscapeToken);

    return LandscapeFlattener.flattenLandscape(
        landscapeToken, ogmApps.stream().map(DefaultApplicationToCityConverter::convert).toList());
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
  @Path("/structure/static/{repositoryName}/{commitHash}")
  public FlatLandscapeDto getStaticStructureData(
      @RestPath final String landscapeToken,
      @RestPath final String repositoryName,
      @RestPath final String commitHash) {
    final Session session = sessionFactory.openSession();

    final List<Application> ogmApps =
        applicationRepository.fetchHydratedApplicationsInRepositoryForCommit(
            session, landscapeToken, repositoryName, commitHash);

    return LandscapeFlattener.flattenLandscape(
        landscapeToken, ogmApps.stream().map(DefaultApplicationToCityConverter::convert).toList());
  }
}
