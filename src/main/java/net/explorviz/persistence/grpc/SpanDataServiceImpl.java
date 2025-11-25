package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import net.explorviz.persistence.proto.SpanData;
import net.explorviz.persistence.proto.SpanDataService;
import net.explorviz.persistence.repository.LandscapeRepository;
import net.explorviz.persistence.repository.SpanRepository;
import net.explorviz.persistence.repository.TraceRepository;

@GrpcService
public class SpanDataServiceImpl implements SpanDataService {

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private SpanRepository spanRepository;

  @Inject
  private TraceRepository traceRepository;

  @Override
  public Uni<Empty> persistSpan(final SpanData spanData) {
    spanRepository.persistSpan(spanData);
    return Uni.createFrom().item(Empty.getDefaultInstance());
  }

}
