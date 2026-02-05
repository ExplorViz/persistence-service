package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Span;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class SpanRepository {

  @Inject
  private ApplicationRepository applicationRepository;

  @Inject
  private FileRevisionRepository fileRevisionRepository;

  @Inject
  private FunctionRepository functionRepository;

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private SessionFactory sessionFactory;

  @Inject
  private TraceRepository traceRepository;

  public Optional<Span> findSpanById(final Session session, final String spanId) {
    return Optional.ofNullable(
        session.queryForObject(Span.class, "MATCH (s:Span {spanId: $spanId}) RETURN s;",
            Map.of("spanId", spanId)));
  }

  public Optional<Span> findSpanById(final String spanId) {
    final Session session = sessionFactory.openSession();
    return findSpanById(session, spanId);
  }

  public Span getOrCreateSpan(final Session session, final String spanId) {
    return findSpanById(session, spanId).orElse(new Span(spanId));
  }
}
