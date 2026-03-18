package net.explorviz.persistence.api.v3;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import net.explorviz.persistence.repository.RepositoryRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/v3/landscapes/{landscapeToken}")
class EvolutionResource {
  @Inject SessionFactory sessionFactory;
  @Inject RepositoryRepository repositoryRepository;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/repositories")
  public List<String> getRepositoryNames(@RestPath final String landscapeToken) {
    final Session session = sessionFactory.openSession();
    return repositoryRepository.fetchAllRepositoryNamesInLandscape(session, landscapeToken);
  }
}
