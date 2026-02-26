package net.explorviz.persistence.api.v2;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import net.explorviz.persistence.api.model.landscape.VisualizationObject;
import net.explorviz.persistence.api.v2.model.landscape.ApplicationDto;
import net.explorviz.persistence.api.v2.model.landscape.LandscapeDto;
import net.explorviz.persistence.api.v2.model.landscape.NodeDto;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.RepositoryRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/v2/landscapes/{landscapeToken}/structure")
@SuppressWarnings("PMD.UseObjectForClearerAPI")
class LandscapeResource {

  @Inject
  private ApplicationRepository applicationRepository;

  @Inject
  private CommitRepository commitRepository;

  @Inject
  private RepositoryRepository repositoryRepository;

  @Inject
  private SessionFactory sessionFactory;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public LandscapeDto getLatestCommit(@RestPath final String landscapeToken,
      @RestPath final String repositoryName,
      @RestPath final String appName,
      @RestPath final String commitHash) {
    final Session session = sessionFactory.openSession();

    final List<Application> ogmApps =
        applicationRepository.fetchAllFullyHydratedApplications(session, landscapeToken);

    final NodeDto node = new NodeDto("", "", ogmApps.stream().map(ApplicationDto::new).toList());
    return new LandscapeDto(landscapeToken, List.of(node), List.of());
  }

  @GET
  @Path("/{commitHash}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<VisualizationObject> getLandscapeStructureForCommit(
      @RestPath final String landscapeToken,
      @RestPath final String commitHash) {
    final Session session = sessionFactory.openSession();

    final Repository hydratedRepo =
        repositoryRepository.getFullyHydratedRepositoryForCommit(session, landscapeToken,
            commitHash).orElseThrow(NotFoundException::new);

    return hydratedRepo.flatten().toList();
  }
}
