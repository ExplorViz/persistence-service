package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Span;
import net.explorviz.persistence.ogm.Trace;
import net.explorviz.persistence.proto.SpanData;
import org.jboss.logging.Logger;
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

  private static final Logger LOGGER = Logger.getLogger(SpanRepository.class);

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

  /* TODO: Handle optional commitId
    If span has not commitId and a FileRevision with same id with no connected Commit exits, then
    nothing happens, otherwise if no FileRevision exists or if all existing FileRevisions already
    have a Commit attached create new
    If span has commitId and no FileRevision with same id -> create FileRevision and
    connect to Commit
      -> create Commit if no Commit with commitId exists
   */
  public void persistSpan(final SpanData spanData) {
    final Session session = sessionFactory.openSession();

    final Span span = new Span(spanData);

    if (!spanData.getParentId().isEmpty()) {
      final Span parentSpan = getOrCreateSpan(session, spanData.getParentId());
      span.setParentSpan(parentSpan);
    }

    final Trace trace = traceRepository.getOrCreateTrace(session, spanData.getTraceId());
    trace.addChildSpan(span);

    final Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, spanData.getLandscapeTokenId());
    landscape.addTrace(trace);

    final Application application =
        applicationRepository.getOrCreateApplication(session, spanData.getApplicationName(),
            spanData.getLandscapeTokenId());

    if (application.getRootDirectory() == null) {
      final Directory applicationRoot = new Directory("*");
      application.setRootDirectory(applicationRoot);
    }
    landscape.addApplication(application);

    session.save(application);

    final Function function =
        functionRepository.getOrCreateFunction(session, spanData.getFunctionFqn(),
            spanData.getLandscapeTokenId());
    span.setFunction(function);

    try {
      fileRevisionRepository.createFileStructureFromFunction(session, function, application,
          landscape);
    } catch (NoSuchElementException e) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Error while persisting span: " + e);
      }
    }

    session.save(List.of(span, trace, landscape, function));
  }
}
