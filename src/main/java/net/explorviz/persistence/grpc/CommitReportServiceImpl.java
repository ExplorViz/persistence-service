package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.proto.CommitReportData;
import net.explorviz.persistence.proto.CommitReportService;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.BranchRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import net.explorviz.persistence.repository.RepositoryRepository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class CommitReportServiceImpl implements CommitReportService {

  @Inject
  private ApplicationRepository applicationRepository;

  @Inject
  private BranchRepository branchRepository;

  @Inject
  private CommitRepository commitRepository;

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private RepositoryRepository repositoryRepository;

  @Inject
  private FileRevisionRepository fileRevisionRepository;

  @Inject
  private SessionFactory sessionFactory;

  private static final String NO_PARENT_ID = "NONE";

  @Override
  public Uni<Empty> sendCommitReport(final CommitReportData request) {
    List<String> files = request.getFilesList();
    List<String> added = request.getAddedList();
    List<String> modified = request.getModifiedList();
    List<String> deleted = request.getDeletedList();

    List<String> unchangedFiles = files.stream().filter(
            f -> !Stream.of(added, modified, deleted).flatMap(List::stream).toList().contains(f))
        .toList();

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

    final Application application =
        applicationRepository.getOrCreateApplication(session, request.getApplicationName(),
            request.getLandscapeToken());

    if (application.getRootDirectory() == null) {
      final Directory applicationRoot = new Directory("*");
      application.setRootDirectory(applicationRoot);
    }
    landscape.addApplication(application);

    for (String f : added) {
      System.out.println(f);
      FileRevision file = fileRevisionRepository.createFileStructureFromFilePath(session, f,
          application, landscape);
      System.out.println(file);
      commit.addFileRevision(file);
    }

    for (String f : modified) {
      System.out.println(f);
      FileRevision file = fileRevisionRepository.createFileStructureFromFilePath(session, f,
          application, landscape);
      System.out.println(file);
      commit.addFileRevision(file);
    }

    if (request.getParentCommitID().isEmpty() || NO_PARENT_ID.equals(request.getParentCommitID())) {
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
