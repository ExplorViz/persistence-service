package net.explorviz.persistence.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.List;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.proto.StateData;
import net.explorviz.persistence.proto.StateDataRequest;
import net.explorviz.persistence.proto.StateDataService;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.BranchRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.DirectoryRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import net.explorviz.persistence.repository.RepositoryRepository;
import net.explorviz.persistence.util.GrpcExceptionMapper;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

@GrpcService
public class StateDataServiceImpl implements StateDataService {

  @Inject private SessionFactory sessionFactory;

  @Inject private ApplicationRepository applicationRepository;

  @Inject private BranchRepository branchRepository;

  @Inject private CommitRepository commitRepository;

  @Inject private DirectoryRepository directoryRepository;

  @Inject private LandscapeRepository landscapeRepository;

  @Inject private RepositoryRepository repositoryRepository;

  @Blocking
  @Override
  public Uni<StateData> getStateData(final StateDataRequest request) {
    final Session session = sessionFactory.openSession();

    try (Transaction tx = session.beginTransaction()) {
      saveStateData(session, request);
      tx.commit();

      final StateData.Builder stateDataBuilder = StateData.newBuilder();
      final String commitId =
          commitRepository
              .findLatestFullyPersistedCommit(
                  session,
                  request.getRepositoryName(),
                  request.getLandscapeToken(),
                  request.getBranchName())
              .map(Commit::getHash)
              .orElse("");
      stateDataBuilder.setCommitId(commitId);

      return Uni.createFrom().item(stateDataBuilder.build());
    } catch (Exception e) { // NOPMD - intentional: Handling in GGrpcExceptionMapper
      return Uni.createFrom().failure(GrpcExceptionMapper.mapToGrpcException(e, request));
    }
  }

  public void saveStateData(final Session session, final StateDataRequest request) {
    final Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, request.getLandscapeToken());

    final Repository repository =
        repositoryRepository.getOrCreateRepository(
            session, request.getRepositoryName(), request.getLandscapeToken());
    landscape.addRepository(repository);

    final Branch branch =
        branchRepository.getOrCreateBranch(
            session,
            request.getBranchName(),
            request.getRepositoryName(),
            request.getLandscapeToken());

    repository.addBranch(branch);

    if (repository.getRootDirectory() == null) {
      final Directory repoRootDirectory = new Directory(request.getRepositoryName());
      repository.setRootDirectory(repoRootDirectory);
    }

    session.save(List.of(repository, landscape));

    request
        .getApplicationPathsMap()
        .forEach(
            (String k, String v) -> {
              final Application application =
                  applicationRepository
                      .findApplicationByNameAndLandscapeToken(
                          session, k, request.getLandscapeToken())
                      .orElse(new Application(k));

              if (v.isEmpty()) {
                application.setRootDirectory(repository.getRootDirectory());
              } else {
                final Directory applicationRootDirectory =
                    directoryRepository.createDirectoryStructureAndReturnLastDirStaticData(
                        session,
                        (repository.getName() + "/" + v).split("/"),
                        request.getRepositoryName(),
                        request.getLandscapeToken());
                application.setRootDirectory(applicationRootDirectory);
              }
              session.save(application);
              session.clear();
            });

    session.save(List.of(repository, landscape));
  }
}
