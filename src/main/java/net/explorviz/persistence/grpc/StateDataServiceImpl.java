package net.explorviz.persistence.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.List;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.proto.StateData;
import net.explorviz.persistence.proto.StateDataRequest;
import net.explorviz.persistence.proto.StateDataService;
import net.explorviz.persistence.repository.LandscapeRepository;
import net.explorviz.persistence.repository.RepositoryRepository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class StateDataServiceImpl implements StateDataService {

  @Inject
  private SessionFactory sessionFactory;

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private RepositoryRepository repositoryRepository;

  @Override
  public Uni<StateData> requestStateData(final StateDataRequest request) {
    final String repoName = Repository.stripRepoNameFromUpstreamName(request.getUpstreamName());

    final Session session = sessionFactory.openSession();

    final Repository repository =
        repositoryRepository.getOrCreateRepository(session, repoName, request.getLandscapeToken());

    final Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, request.getLandscapeToken());
    landscape.addRepository(repository);

    session.save(List.of(repository, landscape));

    final StateData.Builder stateDataBuilder = StateData.newBuilder();
    stateDataBuilder.setBranchName(request.getBranchName());
    stateDataBuilder.setCommitID("");
    return Uni.createFrom().item(stateDataBuilder.build());
  }
}
