package net.explorviz.persistence.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.repository.CommitRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@Path("/{landscapeToken}/commits")
@SuppressWarnings("PMD.UseObjectForClearerAPI")
class CommitResource {

  @Inject
  private CommitRepository commitRepository;

  @Inject
  private SessionFactory sessionFactory;

  @GET
  @Path("/{repositoryName}/{branchName}/latest")
  @Produces(MediaType.APPLICATION_JSON)
  public Commit getLatestCommit(@RestPath final String landscapeToken,
      @RestPath final String repositoryName,
      @RestPath final String branchName) {
    final Session session = sessionFactory.openSession();
    final Optional<Commit> latestCommit =
        commitRepository.findLatestFullyPersistedCommit(session,
            repositoryName,
            landscapeToken, branchName);
    if (latestCommit.isEmpty()) {
      throw new NotFoundException("No commit present for repository: " + repositoryName);
    }
    return latestCommit.get();
  }
}
