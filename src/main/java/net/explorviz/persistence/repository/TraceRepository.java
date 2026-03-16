package net.explorviz.persistence.repository;

import com.google.common.collect.Lists;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  /**
   * Find all traces in a landscape within the given time range.
   *
   * @param session        OGM session object
   * @param landscapeToken String identifier of the visualization landscape
   * @param from           Lower bound of time range to include (epoch nanosecond value)
   * @param to             Upper bound of time range to include (epoch nanosecond value)
   * @return List of traces in the landscape within the time range, hydrated to include all
   *     contained spans and the functions they represent
   */
  public List<Trace> findHydratedTraces(final Session session, final String landscapeToken,
      final long from, final long to) {
    return Lists.newArrayList(session.query(Trace.class, """
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(t:Trace)
        WHERE
          t.startTime >= $from AND t.endTime <= $to
        CALL apoc.path.subgraphAll(t, {
          relationshipFilter: "CONTAINS>|REPRESENTS>|HAS_PARENT>"
        })
        YIELD relationships
        UNWIND relationships as r
        RETURN startNode(r), r, endNode(r);
        """, Map.of("tokenId", landscapeToken, "from", from, "to", to)));
  }

  /**
   * Finds an associated commit hash for a trace by looking for any file referenced by the trace
   * that is contained in a commit. The query operates under the assumption that for any given
   * trace, there will be at most one such associated commit hash to be found, and therefore returns
   * the first match it discovers.
   *
   * @param session        OGM session object
   * @param landscapeToken String identifier of the visualization landscape
   * @param traceId        OpenTelemetry trace ID
   * @return Optional describing the commit hash string. Empty if no match is found
   */
  public Optional<String> findCommitHashForTrace(final Session session, final String landscapeToken,
      final String traceId) {
    return Optional.ofNullable(session.queryForObject(String.class, """
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(t:Trace {traceId: $traceId})
        MATCH (t)
          -[:CONTAINS]->(:Span)
          -[:REPRESENTS]->(:Function)
          <-[:CONTAINS]-(:FileRevision)
          <-[:CONTAINS]-(c:Commit)
        LIMIT 1
        RETURN c.hash;
        """, Map.of("tokenId", landscapeToken, "traceId", traceId)));
  }

  public Trace getOrCreateTrace(final Session session, final String traceId) {
    return findTraceById(session, traceId).orElse(new Trace(traceId));
  }
}
