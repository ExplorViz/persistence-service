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

  @Inject private SessionFactory sessionFactory;

  public Optional<Trace> findTraceById(final Session session, final String traceId) {
    return Optional.ofNullable(
        session.queryForObject(
            Trace.class,
            "MATCH (t:Trace {traceId: $traceId}) RETURN t;",
            Map.of("traceId", traceId)));
  }

  public Optional<Trace> findTraceById(final String traceId) {
    final Session session = sessionFactory.openSession();
    return findTraceById(session, traceId);
  }

  public Trace getOrCreateTrace(final Session session, final String traceId) {
    return findTraceById(session, traceId).orElse(new Trace(traceId));
  }

  public List<TimestampDto> findTimestampsForLandscapeTokenCommitAndTimeRange(
      final Session session,
      final String landscapeToken,
      final long newest,
      final long oldest,
      final String commitHash,
      final long bucketSize) {
    return session.queryDto(
        """
        MATCH (l:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(t:Trace)
          -[:CONTAINS]->(s:Span)
        WHERE
          (s)-[:REPRESENT]->(:Function)
            <-[:CONTAINS]-(:FileRevision)
            <-[:CONTAINS]-(:Commit {hash: $commitHash}) AND
          s.startTime >= $oldest AND s.startTime <= $newest
        WITH
          (toInteger(s.startTime / $bucketSize)) AS bucket
        RETURN bucket AS epochNano, COUNT(s) AS spanCount
        ORDER BY bucket ASC;
        """,
        Map.of(
            "tokenId", landscapeToken,
            "newest", newest,
            "oldest", oldest,
            "commitHash", commitHash,
            "bucketSize", bucketSize),
        TimestampDto.class);
  }

  public List<TimestampDto> findTimestampsForLandscapeTokenCommitAndTimeRange(
      final Session session,
      final String landscapeToken,
      final long newest,
      final long oldest,
      final long bucketSize) {
    return session.queryDto(
        """
            MATCH (l:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(t:Trace)
              -[:CONTAINS]->(s:Span)
            WHERE
              s.startTime >= $oldest AND s.startTime <= $newest
            WITH
              (toInteger(s.startTime / $bucketSize)) AS bucket
            RETURN bucket AS epochNano, COUNT(s) AS spanCount
            ORDER BY bucket ASC;
            """,
        Map.of(
            "tokenId",
            landscapeToken,
            "newest",
            newest,
            "oldest",
            oldest,
            "bucketSize",
            bucketSize),
        TimestampDto.class);
  }

  public void deleteTraceData(final Session session, final String landscapeToken) {
    session.query(
        """
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
        """,
        Map.of("tokenId", landscapeToken));
  }
}
