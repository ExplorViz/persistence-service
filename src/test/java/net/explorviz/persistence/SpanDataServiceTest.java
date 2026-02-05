package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.proto.SpanData;
import net.explorviz.persistence.proto.SpanDataService;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class SpanDataServiceTest {
  @GrpcClient
  SpanDataService spanDataService;

  @Inject
  CommitRepository commitRepository;

  @Inject
  FileRevisionRepository fileRevisionRepository;

  @Inject
  LandscapeRepository landscapeRepository;

  @Inject
  SessionFactory sessionFactory;

  @BeforeEach
  void cleanup() {
    Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  @Test
  void testPersistSpan() {
    SpanData testSpanData =
        SpanData.newBuilder()
            .setSpanId("mySpan")
            .setTraceId("myTrace")
            .setApplicationName("hello-world")
            .setLandscapeTokenId("mytokenvalue")
            .setFunctionFqn("net.explorviz.helloworld.MyClass.myMethod")
            .setStartTime(1)
            .setEndTime(5)
            .build();

    Empty reply = spanDataService.persistSpan(testSpanData).await()
        .atMost(Duration.ofSeconds(5));
    assertNotNull(reply);

    Session session = sessionFactory.openSession();

    Application result = session.queryForObject(Application.class, """
        MATCH (app:Application {name: 'hello-world'})
              -[:HAS_ROOT]->(:Directory)
              -[:CONTAINS]->(:Directory {name: 'net'})
              -[:CONTAINS]->(:Directory {name: 'explorviz'})
              -[:CONTAINS]->(:Directory {name: 'helloworld'})
              -[:CONTAINS]->(:FileRevision {name: 'MyClass'})
              -[:CONTAINS]->(:Function {name: 'myMethod'})
              <-[:REPRESENTS]-(:Span {spanId: 'mySpan'})
              <-[:CONTAINS]-(:Trace {traceId: 'myTrace'})
              <-[:CONTAINS]-(:Landscape {tokenId: 'mytokenvalue'})
        RETURN app;""", Map.of());

    assertNotNull(result);
  }

  /**
   * If a commit hash is included in the span, the corresponding node should be connected / created.
   */
  @Test
  void testPersistSpanWithCommit() {
    SpanData testSpanData =
        SpanData.newBuilder()
            .setSpanId("mySpan")
            .setTraceId("myTrace")
            .setApplicationName("hello-world")
            .setLandscapeTokenId("mytokenvalue")
            .setFunctionFqn("net.explorviz.helloworld.MyClass.myMethod")
            .setStartTime(1)
            .setEndTime(5)
            .setCommitId("commit1")
            .build();

    Empty reply = spanDataService.persistSpan(testSpanData).await()
        .atMost(Duration.ofSeconds(5));
    assertNotNull(reply);

    Session session = sessionFactory.openSession();

    Commit commit = session.queryForObject(Commit.class, """
        MATCH (:Application {name: 'hello-world'})
              -[:HAS_ROOT]->(:Directory)
              -[:CONTAINS]->(:Directory {name: 'net'})
              -[:CONTAINS]->(:Directory {name: 'explorviz'})
              -[:CONTAINS]->(:Directory {name: 'helloworld'})
              -[:CONTAINS]->(file:FileRevision {name: 'MyClass'})
              -[:CONTAINS]->(:Function {name: 'myMethod'})
              <-[:REPRESENTS]-(:Span {spanId: 'mySpan'})
              <-[:CONTAINS]-(:Trace {traceId: 'myTrace'})
              <-[:CONTAINS]-(:Landscape {tokenId: 'mytokenvalue'})
        MATCH (commit:Commit {hash: 'commit1'})-[:CONTAINS]->(file)
        RETURN commit;""", Map.of());

    assertNotNull(commit);
  }

  /**
   * Persisting the same span twice should create no additional nodes.
   */
  @Test
  void testPersistSpanIdempotent() {
    SpanData testSpanData =
        SpanData.newBuilder()
            .setSpanId("mySpan")
            .setTraceId("myTrace")
            .setApplicationName("hello-world")
            .setLandscapeTokenId("mytokenvalue")
            .setFunctionFqn("net.explorviz.helloworld.MyClass.myMethod")
            .setStartTime(1)
            .setEndTime(5)
            .build();

    Empty reply = spanDataService.persistSpan(testSpanData).await()
        .atMost(Duration.ofSeconds(5));
    assertNotNull(reply);

    reply = spanDataService.persistSpan(testSpanData).await()
        .atMost(Duration.ofSeconds(5));
    assertNotNull(reply);

    Session session = sessionFactory.openSession();

    Result result = session.query("""
        RETURN
          COUNT {(:FileRevision)} AS files,
          COUNT {(:Application)} AS apps,
          COUNT {(:Span)} AS spans,
          COUNT {(:Trace)} AS traces""", Map.of());

    Map<String, Object> countMap = result.queryResults().iterator().next();
    assertEquals(1, (Long) countMap.get("files"));
    assertEquals(1, (Long) countMap.get("apps"));
    assertEquals(1, (Long) countMap.get("spans"));
    assertEquals(1, (Long) countMap.get("traces"));
  }

}
