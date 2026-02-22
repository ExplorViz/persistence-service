package net.explorviz.persistence.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import net.explorviz.persistence.api.model.landscape.VisualizationObject;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.RepositoryRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/v2/landscapes/{landscapeToken}/structure")
class LandscapeResource {

  @Inject
  private CommitRepository commitRepository;

  @Inject
  private RepositoryRepository repositoryRepository;

  @Inject
  private SessionFactory sessionFactory;

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
