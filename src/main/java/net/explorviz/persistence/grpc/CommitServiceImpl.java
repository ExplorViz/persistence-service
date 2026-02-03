package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.List;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.ogm.Tag;
import net.explorviz.persistence.proto.CommitData;
import net.explorviz.persistence.proto.CommitService;
import net.explorviz.persistence.proto.FileIdentifier;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.BranchRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import net.explorviz.persistence.repository.RepositoryRepository;
import net.explorviz.persistence.repository.TagRepository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class CommitServiceImpl implements CommitService {

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
  private TagRepository tagRepository;

  @Inject
  private SessionFactory sessionFactory;

  private static final String NO_PARENT_ID = "NONE";

  @Blocking
  @Override
  public Uni<Empty> persistCommit(final CommitData request) {
    final Session session = sessionFactory.openSession();

    final Repository repo = repositoryRepository.findRepositoryByNameAndLandscapeToken(session,
        request.getRepositoryName(), request.getLandscapeToken()).orElse(null);
    if (repo == null) {
      return Uni.createFrom().failure(
          Status.FAILED_PRECONDITION.withDescription("No corresponding state data was sent before.")
              .asRuntimeException());
    }

    final Branch branch = branchRepository.getOrCreateBranch(session, request.getBranchName(),
        request.getRepositoryName(), request.getLandscapeToken());
    repo.addBranch(branch);

    final Commit commit = commitRepository.getOrCreateCommit(session, request.getCommitId(),
        request.getLandscapeToken());
    commit.setBranch(branch);
    repo.addCommit(commit);

    for (final FileIdentifier f : request.getAddedFilesList()) {
      final FileRevision file = fileRevisionRepository.createFileStructureFromStaticData(session, f,
          request.getRepositoryName(), request.getLandscapeToken(), commit);
      commit.addFileRevision(file);
    }

    for (final FileIdentifier f : request.getModifiedFilesList()) {
      final FileRevision file = fileRevisionRepository.createFileStructureFromStaticData(session, f,
          request.getRepositoryName(), request.getLandscapeToken(), commit);
      commit.addFileRevision(file);
    }

    for (final FileIdentifier f : request.getUnchangedFilesList()) {
      final FileRevision file =
          fileRevisionRepository.getFileRevisionFromHash(session, f.getFileHash(),
              request.getRepositoryName(), request.getLandscapeToken()).orElseGet(
                () -> fileRevisionRepository.createFileStructureFromStaticData(session, f,
                  request.getRepositoryName(), request.getLandscapeToken(), commit));
      commit.addFileRevision(file);
    }

    for (final String tagName : request.getTagsList()) {
      final Tag tag =
          tagRepository.findTagByName(session, tagName).orElse(new Tag(tagName));
      commit.addTag(tag);
    }

    if (request.getParentCommitId().isEmpty() || NO_PARENT_ID.equals(request.getParentCommitId())) {
      session.save(List.of(repo, branch, commit));
    } else {
      final Commit parentCommit =
          commitRepository.getOrCreateCommit(session, request.getParentCommitId(),
              request.getLandscapeToken());
      commit.addParent(parentCommit);
      session.save(List.of(repo, branch, commit, parentCommit));
    }

    return Uni.createFrom().item(() -> Empty.newBuilder().build());
  }
}
