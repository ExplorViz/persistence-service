package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import net.explorviz.persistence.proto.FileData;
import net.explorviz.persistence.proto.FileDataService;
import net.explorviz.persistence.repository.FileRevisionRepository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class FileDataServiceImpl implements FileDataService {

  @Inject
  private FileRevisionRepository fileRevisionRepository;

  @Inject
  private SessionFactory sessionFactory;

  @Override
  public Uni<Empty> sendFileData(final FileData request) {
    return Uni.createFrom().item(Empty.getDefaultInstance());

//    final Session session = sessionFactory.openSession();
//    fileRevisionRepository.createFileStructureFromFileData(session, request);
//    return Uni.createFrom().item(Empty.getDefaultInstance());
  }
}
