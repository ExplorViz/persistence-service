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
  BranchRepository branchRepository;

  @Inject
  CommitRepository commitRepository;

  @Inject
  LandscapeRepository landscapeRepository;

  @Inject
  RepositoryRepository repositoryRepository;

  @Inject
  SessionFactory sessionFactory;

  @Override
  public Uni<Empty> sendCommitReport(final CommitReportData request) {
    Session session = sessionFactory.openSession();

    String repoName = Repository.stripRepoNameFromUpstreamName(request.getRepositoryName());
    Repository repo =
        repositoryRepository.getOrCreateRepository(session, repoName,
            request.getLandscapeToken());

    Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, request.getLandscapeToken());
    landscape.addRepository(repo);

    Branch branch =
        branchRepository.getOrCreateBranch(session, request.getBranchName(),
            repoName,
            request.getLandscapeToken());
    repo.addBranch(branch);

    Commit commit = commitRepository.getOrCreateCommit(session, request.getCommitID(),
        request.getLandscapeToken());
    commit.setBranch(branch);
    repo.addCommit(commit);

    if (!request.getParentCommitID().isEmpty()) {
      Commit parentCommit = commitRepository.getOrCreateCommit(session, request.getParentCommitID(),
          request.getLandscapeToken());
      commit.addParent(parentCommit);
      session.save(List.of(landscape, repo, branch, commit, parentCommit));
    } else {
      session.save(List.of(landscape, repo, branch, commit));
    }

    return Uni.createFrom().item(() -> Empty.newBuilder().build());
  }
}
