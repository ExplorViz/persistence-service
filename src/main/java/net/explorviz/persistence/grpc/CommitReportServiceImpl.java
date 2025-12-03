package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.List;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.proto.CommitReportData;
import net.explorviz.persistence.proto.CommitReportService;
import net.explorviz.persistence.repository.BranchRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import net.explorviz.persistence.repository.RepositoryRepository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class CommitReportServiceImpl implements CommitReportService {

  @Inject
  private BranchRepository branchRepository;

  @Inject
  private CommitRepository commitRepository;

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private RepositoryRepository repositoryRepository;

  @Inject
  private SessionFactory sessionFactory;

  @Override
  public Uni<Empty> sendCommitReport(final CommitReportData request) {
    final Session session = sessionFactory.openSession();

    final String repoName = Repository.stripRepoNameFromUpstreamName(request.getRepositoryName());
    final Repository repo =
        repositoryRepository.getOrCreateRepository(session, repoName, request.getLandscapeToken());

    final Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, request.getLandscapeToken());
    landscape.addRepository(repo);

    final Branch branch =
        branchRepository.getOrCreateBranch(session, request.getBranchName(), repoName,
            request.getLandscapeToken());
    repo.addBranch(branch);

    final Commit commit = commitRepository.getOrCreateCommit(session, request.getCommitID(),
        request.getLandscapeToken());
    commit.setBranch(branch);
    repo.addCommit(commit);

    if (request.getParentCommitID().isEmpty()) {
      session.save(List.of(landscape, repo, branch, commit));
    } else {
      final Commit parentCommit =
          commitRepository.getOrCreateCommit(session, request.getParentCommitID(),
              request.getLandscapeToken());
      commit.addParent(parentCommit);
      session.save(List.of(landscape, repo, branch, commit, parentCommit));
    }

    return Uni.createFrom().item(() -> Empty.newBuilder().build());
  }
}
