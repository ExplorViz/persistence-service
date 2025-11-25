package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Span;
import net.explorviz.persistence.ogm.Trace;
import net.explorviz.persistence.proto.SpanData;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class SpanRepository {

  @Inject
  private ApplicationRepository applicationRepository;

  @Inject
  private FunctionRepository functionRepository;

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private SessionFactory sessionFactory;

  @Inject
  private TraceRepository traceRepository;

  public Span findSpanById(final String spanId) {
    final Session session = sessionFactory.openSession();
    return findSpanById(session, spanId);
  }

  public Span findSpanById(final Session session, final String spanId) {
    return session.queryForObject(Span.class, "MATCH (s:Span {spanId: $spanId}) RETURN s;",
        Map.of("spanId", spanId));
  }

  // TODO: Refactor with factory pattern :-)
  public void persistSpan(final SpanData spanData) {
    final Session session = sessionFactory.openSession();

    final Span span = new Span(spanData);

    Span parentSpan = null;
    if (!spanData.getParentId().isEmpty()) {
      parentSpan = findSpanById(session, spanData.getParentId());

      if (parentSpan == null) {
        parentSpan = new Span(spanData.getParentId());
      }
      span.setParentSpan(parentSpan);
    }

    Trace trace = traceRepository.findTraceById(session, spanData.getTraceId());

    if (trace == null) {
      trace = new Trace(spanData.getTraceId(), spanData.getStartTime(), spanData.getEndTime(),
          Set.of(span));
    } else {
      trace.addChildSpan(span);
    }

    Landscape landscape =
        landscapeRepository.findLandscapeByTokenId(session, spanData.getLandscapeTokenId());

    if (landscape == null) {
      landscape = new Landscape(spanData.getLandscapeTokenId(), Set.of(trace));
    } else {
      landscape.addTrace(trace);
    }

    Function function =
        functionRepository.findFunctionByFqnAndLandscapeToken(spanData.getFunctionFqn(),
            spanData.getLandscapeTokenId());

    if (function == null) {
      function = new Function(spanData.getFunctionFqn());
    }
    span.setFunction(function);

    Application application =
        applicationRepository.findApplicationByNameAndLandscapeToken(spanData.getApplicationName(),
            spanData.getLandscapeTokenId());

    if (application == null) {
      application = new Application(spanData.getApplicationName());
    }
    span.setApplication(application);

    if (parentSpan != null) {
      session.save(List.of(span, parentSpan, trace, landscape, function, application));
    } else {
      session.save(List.of(span, trace, landscape, function, application));
    }
  }
}
