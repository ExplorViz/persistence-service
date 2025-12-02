package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
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
  SessionFactory sessionFactory;

  @Inject
  LandscapeRepository landscapeRepository;

  @Inject
  RepositoryRepository repositoryRepository;

  @Override
  public Uni<StateData> requestStateData(final StateDataRequest request) {
    String repoName = Repository.stripRepoNameFromUpstreamName(request.getUpstreamName());

    Session session = sessionFactory.openSession();

    Repository repository =
        repositoryRepository.getOrCreateRepository(session, repoName, request.getLandscapeToken());

    Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, request.getLandscapeToken());
    landscape.addRepository(repository);

    session.save(List.of(repository, landscape));

    final StateData.Builder stateDataBuilder = StateData.newBuilder();
    stateDataBuilder.setBranchName(request.getBranchName());
    stateDataBuilder.setCommitID("");
    return Uni.createFrom().item(stateDataBuilder.build());
  }
}
