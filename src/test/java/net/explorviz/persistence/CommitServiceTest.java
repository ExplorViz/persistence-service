package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.Timestamp;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.proto.CommitData;
import net.explorviz.persistence.proto.CommitService;
import net.explorviz.persistence.proto.FileIdentifier;
import net.explorviz.persistence.proto.StateDataRequest;
import net.explorviz.persistence.proto.StateDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class CommitServiceTest {

  private static final long GRPC_AWAIT_SECONDS = 5;

  @GrpcClient
  CommitService commitService;

  @GrpcClient
  StateDataService stateDataService;

  @Inject
  SessionFactory sessionFactory;

  @BeforeEach
  void cleanup() {
    Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  @Test
  void testFilesAddedFromCommitData() {
    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken("mytokenvalue").setRepositoryName("myrepo")
            .setBranchName("main").build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitData1 =
        CommitData.newBuilder().setCommitId("commit1").setRepositoryName("myrepo")
            .setBranchName("main").setLandscapeToken("mytokenvalue")
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .addAllAddedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash("1").setFilePath("src/File1.java").build(),
                FileIdentifier.newBuilder().setFileHash("2").setFilePath("src/File2.java").build()))
            .build();

    CommitData commitData2 =
        CommitData.newBuilder().setCommitId("commit1").setRepositoryName("myrepo")
            .setBranchName("main").setLandscapeToken("mytokenvalue")
            .setAuthorDate(Timestamp.newBuilder().setSeconds(2).setNanos(200).build())
            .addAllModifiedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash("1").setFilePath("src/File1.java").build(),
                FileIdentifier.newBuilder().setFileHash("2").setFilePath("src/File2.java").build()))
            .build();

    commitService.persistCommit(commitData1).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));
    commitService.persistCommit(commitData2).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    Result result = session.query("MATCH (f:FileRevision) RETURN COUNT(f) AS count;", Map.of());

    assertTrue(result.queryResults().iterator().hasNext());
    assertEquals(2, (Long) result.queryResults().iterator().next().get("count"));
  }
}
