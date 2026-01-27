package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Optional;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.proto.SpanDataService;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class SpanDataServiceTest {
  @GrpcClient
  SpanDataService spanDataService;

  @Inject
  FileRevisionRepository fileRevisionRepository;

  @Inject
  LandscapeRepository landscapeRepository;

  @Inject
  SessionFactory sessionFactory;

  @Test
  void testLandscape() {
    Session session = sessionFactory.openSession();
    Landscape landscape = new Landscape("mytokenvalue");
    session.save(landscape);
    Optional<Landscape> optionalLandscape =
        landscapeRepository.findLandscapeByTokenId(session, "mytokenvalue");
    assertTrue(optionalLandscape.isPresent());
  }

  @Test
  void testPersistSpan() {
//    SpanData testSpanData =
//        SpanData.newBuilder()
//            .setSpanId("myspan")
//            .setTraceId("mytrace")
//            .setApplicationName("hello-world")
//            .setLandscapeTokenId("mytokenvalue")
//            .setFunctionFqn("net.explorviz.helloworld.MyClass.myMethod")
//            .setStartTime(1)
//            .setEndTime(5)
//            .build();
//
//    Empty reply = spanDataService.persistSpan(testSpanData).await()
//        .atMost(Duration.ofSeconds(5));
//
//    String[] splitFqn = testSpanData.getFunctionFqn().split("\\.");
//    String fileName = splitFqn[splitFqn.length - 2];
//
//    Session session = sessionFactory.openSession();
//
//    FileRevision result =
//        session.queryForObject(FileRevision.class, "MATCH (f:FileRevision {name: $name}) RETURN f;",
//            Map.of("name", "MyClass"));
//
//    assertNotNull(reply);
//    assertNotNull(result);
  }

}
