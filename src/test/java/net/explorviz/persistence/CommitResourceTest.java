package net.explorviz.persistence;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import com.google.protobuf.Timestamp;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import net.explorviz.persistence.proto.CommitData;
import net.explorviz.persistence.proto.CommitService;
import net.explorviz.persistence.proto.FileData;
import net.explorviz.persistence.proto.FileDataService;
import net.explorviz.persistence.proto.FileIdentifier;
import net.explorviz.persistence.proto.StateDataRequest;
import net.explorviz.persistence.proto.StateDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class CommitResourceTest {

  private static final int GRPC_AWAIT_SECONDS = 5;

  @GrpcClient
  CommitService commitService;

  @GrpcClient
  FileDataService fileDataService;

  @GrpcClient
  StateDataService stateDataService;

  @Inject
  SessionFactory sessionFactory;

  @BeforeEach
  void cleanup() {
    Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  // Example test for a REST endpoint. Will be removed once API is implemented.
  @Test
  void testGetLatestCommit() {
    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken("mytokenvalue").setRepositoryName("myrepo")
            .setBranchName("main").build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitData1 =
        CommitData.newBuilder().setCommitId("commit1").setRepositoryName("myrepo")
            .setBranchName("main").setLandscapeToken("mytokenvalue")
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .addAddedFiles(
                FileIdentifier.newBuilder().setFileHash("1").setFilePath("src/File1.java").build())
            .build();

    CommitData commitData2 =
        CommitData.newBuilder().setCommitId("commit2").setParentCommitId("commit1")
            .setRepositoryName("myrepo").setBranchName("main").setLandscapeToken("mytokenvalue")
            .setAuthorDate(Timestamp.newBuilder().setSeconds(2).setNanos(200).build())
            .addAddedFiles(
                FileIdentifier.newBuilder().setFileHash("2").setFilePath("src/File2.java").build())
            .addModifiedFiles(
                FileIdentifier.newBuilder().setFileHash("1").setFilePath("src/File1.java").build())
            .build();

    commitService.persistCommit(commitData1).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));
    commitService.persistCommit(commitData2).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    FileData fileData =
        FileData.newBuilder().setFileHash("1").setFilePath("src/File1.java")
            .setRepositoryName("myrepo").setLandscapeToken("mytokenvalue").build();

    fileDataService.persistFile(fileData).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    given().when().get("/mytokenvalue/commits/myrepo/main/latest").then().assertThat()
        .body("hash", equalTo("commit1"));
  }
}
