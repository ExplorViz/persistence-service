package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.api.v2.model.TimestampDto;
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

  public List<TimestampDto> findTimestampsForLandscapeTokenCommitAndTimeRange(final Session session,
      final String landscapeToken, final long newest, final long oldest, final String commitHash) {
    return session.queryDto("""
        MATCH (l:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(t:Trace)
          -[:CONTAINS]->(s:Span)
        WHERE
          (s)-[:REPRESENT]->(:Function)
            <-[:CONTAINS]-(:FileRevision)
            <-[:CONTAINS]-(:Commit {hash: $commitHash) AND
          s.start_time >= $oldest AND s.start_time <= $newest
        WITH
          (toInteger(s.start_time / 1000000000)) AS bucket
        MATCH (l)
          -[:CONTAINS]->(:Trace)
          -[:CONTAINS]->(s2:Span)
        WHERE
          toInteger(s2.start_time / 1000000000) = bucket
        RETURN DISTINCT bucket AS epochNano, COUNT(s2) AS spanCount
        ORDER BY bucket ASC;
        """, Map.of("tokenId", landscapeToken, "newest", newest, "oldest", oldest, "commitHash",
        commitHash), TimestampDto.class);
  }

  public List<TimestampDto> findTimestampsForLandscapeTokenCommitAndTimeRange(final Session session,
      final String landscapeToken, final long newest, final long oldest) {
    return session.queryDto("""
            MATCH (l:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(t:Trace)
              -[:CONTAINS]->(s:Span)
            WHERE
              s.start_time >= $oldest AND s.start_time <= $newest
            WITH
              (toInteger(s.start_time / 1000000000)) AS bucket
            MATCH (l)
              -[:CONTAINS]->(:Trace)
              -[:CONTAINS]->(s2:Span)
            WHERE
              toInteger(s2.start_time / 1000000000) = bucket
            RETURN DISTINCT bucket AS epochNano, COUNT(s2) AS spanCount
            ORDER BY bucket ASC;
            """, Map.of("tokenId", landscapeToken, "newest", newest, "oldest", oldest),
        TimestampDto.class);
  }

  public void deleteTraceData(final Session session, final String landscapeToken) {
    session.query("""
        MATCH (:Landscape {tokenId: tokenId})
          -[:CONTAINS]->(t:Trace)
          -[:CONTAINS]->(s:Span)
        MATCH (fr:FileRevision)
          -[:CONTAINS]->(f:Function)
        WHERE
          (s)-[:REPRESENTS]->(f) AND
          NOT (:Commit)-[:CONTAINS]->(fr)
        CALL apoc.path.subgraphAll(fr, {
          relationshipFilter: "CONTAINS>"
        })
        YIELD nodes
        UNWIND nodes as n
        DETACH DELETE n, t, s, f, fr;
        """, Map.of("tokenId", landscapeToken));
  }
}
