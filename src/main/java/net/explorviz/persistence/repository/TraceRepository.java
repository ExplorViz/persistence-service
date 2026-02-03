package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.api.model.Timestamp;
import net.explorviz.persistence.ogm.Trace;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class TraceRepository {

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Trace> findTraceById(final Session session, final String traceId) {
    return Optional.ofNullable(
        session.queryForObject(Trace.class, "MATCH (t:Trace {traceId: $traceId}) RETURN t;",
            Map.of("traceId", traceId)));
  }

  public Optional<Trace> findTraceById(final String traceId) {
    final Session session = sessionFactory.openSession();
    return findTraceById(session, traceId);
  }

  public Trace getOrCreateTrace(final Session session, final String traceId) {
    return findTraceById(session, traceId).orElse(new Trace(traceId));
  }

  public List<Timestamp> getTraceTimestamps(final String landscapeToken) {
    final Session session = sessionFactory.openSession();
    final Iterable<Map<String, Object>> result = session.query("""
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(t:Trace)
        OPTIONAL MATCH (t)-[:CONTAINS]->(s:Span)
        RETURN t.startTime as epochNano, count(s) as spanCount
        ORDER BY t.startTime ASC
        """, Map.of("tokenId", landscapeToken));
    
    final List<Timestamp> timestamps = new ArrayList<>();
    for (final Map<String, Object> row : result) {
      final Long epochNano = (Long) row.get("epochNano");
      final Number spanCount = (Number) row.get("spanCount");
      if (epochNano != null && spanCount != null) {
        timestamps.add(new Timestamp(epochNano, spanCount.intValue()));
      }
    }
    if (timestamps.isEmpty()) {
      java.time.Instant now = java.time.Instant.now();
      long currentEpochNano = now.getEpochSecond() * 1_000_000_000L + now.getNano();
      timestamps.add(new Timestamp(currentEpochNano, 0));
    }
    return timestamps;
  }
}
