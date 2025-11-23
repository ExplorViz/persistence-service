package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import net.explorviz.persistence.ogm.Span;
import net.explorviz.persistence.proto.SpanData;
import net.explorviz.persistence.proto.SpanDataService;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class SpanDataServiceImpl implements SpanDataService {

  @Inject
  private SessionFactory sessionFactory;

  @Override
  public Uni<Empty> persistSpan(final SpanData spanData) {
    final Session session = sessionFactory.openSession();
    final Span span = new Span(spanData);
    session.save(span);
    return Uni.createFrom().item(Empty.getDefaultInstance());
  }

}
