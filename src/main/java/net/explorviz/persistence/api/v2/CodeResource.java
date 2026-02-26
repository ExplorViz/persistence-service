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
import java.util.Set;
import net.explorviz.persistence.api.v2.model.BranchDto;
import net.explorviz.persistence.api.v2.model.BranchPointDto;
import net.explorviz.persistence.api.v2.model.CommitTreeDto;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.CommitRepository;
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
  private CommitRepository commitRepository;

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

      parentCommits.stream().filter(pc -> !branchName.equals(pc.getBranch().getName())).findFirst()
          .ifPresent(parentCommit -> branchPointMap.putIfAbsent(branchName,
              new BranchPointDto(parentCommit.getHash(), parentCommit.getBranch().getName())));
    }

    final List<BranchDto> branches = commitsMap.entrySet().stream()
        .map(e -> new BranchDto(e.getKey(), e.getValue(), branchPointMap.get(e.getKey()))).toList();

    return new CommitTreeDto(applicationName, branches);
  }
}
