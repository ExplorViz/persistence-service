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
    if (commitHash == null) {
      return session.queryDto("""
          MATCH (:Landscape {token: $token})
            -[:CONTAINS]->(t:Trace)
            -[:CONTAINS]->(s:Span)
          WHERE
            s.start_time >= $oldest AND s.start_time <= $newest
          WITH
            (s.start_time / 10000000000) * 10000000000 AS bucket, COUNT(s) AS spanCount
          RETURN bucket AS epochNano, spanCount
          ORDER BY epochNano ASC;
          """, Map.of("token", landscapeToken, "newest", newest, "oldest", oldest),
              TimestampDto.class);
    }

    return session.queryDto("""
        MATCH (:Landscape {token: $token})
          -[:CONTAINS]->(:Repository)
          -[:CONTAINS]->(c:Commit {hash: $commitHash})
          -[:CONTAINS]->(fr:FileRevision)
          -[:CONTAINS]->(f:Function)
          <-[:CONTAINS]-(s:Span)
          <-[:CONTAINS]-(t:Trace)
        WHERE s.start_time >= $oldest AND s.start_time <= $newest
        WITH (s.start_time / 10000000000) * 10000000000 AS bucket, COUNT(s) AS spanCount
        RETURN bucket AS epochNano, spanCount
        ORDER BY epochNano ASC;
        """, Map.of("token", landscapeToken, "newest", newest, "oldest", oldest, "commitHash",
        commitHash), TimestampDto.class);
  }

  public void deleteTraceData(final Session session, final String landscapeToken) {
    session.query("""
        MATCH (:Landscape {landscapeToken: $landscapeToken})
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
        """, Map.of("landscapeToken", landscapeToken));
  }
}
