package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import net.explorviz.persistence.proto.FileData;
import net.explorviz.persistence.proto.FileDataService;

@GrpcService
public class FileDataServiceImpl implements FileDataService {
  @Override
  public Uni<Empty> sendFileData(final FileData request) {
    return Uni.createFrom().item(Empty.getDefaultInstance());
  }
}
