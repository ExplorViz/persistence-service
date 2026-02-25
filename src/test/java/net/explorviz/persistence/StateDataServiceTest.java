package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.protobuf.Timestamp;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.proto.CommitData;
import net.explorviz.persistence.proto.CommitService;
import net.explorviz.persistence.proto.FileData;
import net.explorviz.persistence.proto.FileDataService;
import net.explorviz.persistence.proto.FileIdentifier;
import net.explorviz.persistence.proto.Language;
import net.explorviz.persistence.proto.StateData;
import net.explorviz.persistence.proto.StateDataRequest;
import net.explorviz.persistence.proto.StateDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class StateDataServiceTest {

  private static final long GRPC_AWAIT_SECONDS = 5;

  @GrpcClient
  CommitService commitService;

  @GrpcClient
  FileDataService fileDataService;

  @GrpcClient
  StateDataService stateDataService;

  @Inject
  SessionFactory sessionFactory;

  private String landscapeToken;
  private String repoName;
  private String branchName;

  @BeforeEach
  void init() {
    Session session = sessionFactory.openSession();
    session.purgeDatabase();
    landscapeToken = "mytokenvalue";
    repoName = "myrepo";
    branchName = "main";
  }

  @Test
  void testGetStateDataOnEmptyDB() {
    String appName = "testApp";

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).putAllApplicationPaths(Map.of(appName, "")).build();

    StateData stateData = stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    Landscape landscape = session.queryForObject(Landscape.class, """
        MATCH (l:Landscape {tokenId: $tokenId})
        RETURN l;
        """, Map.of("tokenId", landscapeToken));

    Repository repository = session.queryForObject(Repository.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(r:Repository {name: $repoName})
        RETURN r;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName));

    Directory rootDirectory = session.queryForObject(Directory.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:HAS_ROOT]->(root:Directory {name: $repoName})
        RETURN root;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName));

    Application application = session.queryForObject(Application.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:HAS_ROOT]->(:Directory {name: $repoName})
              <-[:HAS_ROOT]-(a:Application {name: $appName})
        RETURN a;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName, "appName", appName));

    Branch branch = session.queryForObject(Branch.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:CONTAINS]->(b:Branch {name: $branchName})
        RETURN b;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName, "branchName", branchName));

    assertEquals("", stateData.getCommitId());
    assertNotNull(landscape);
    assertNotNull(repository);
    assertNotNull(rootDirectory);
    assertNotNull(application);
    assertNotNull(branch);
  }

  @Test
  void testGetStateDataWithMultipleApplications() {
    String appNameOne = "testAppOne";
    String appPathOne = "src/" + appNameOne;
    String appNameTwo = "testAppTwo";
    String appPathTwo = "src/org/" + appNameTwo;

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName)
            .putAllApplicationPaths(Map.of(appNameOne, appPathOne, appNameTwo, appPathTwo)).build();

    StateData stateData = stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    Application applicationOne = session.queryForObject(Application.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:HAS_ROOT]->(:Directory {name: $repoName})
              -[:CONTAINS]->(:Directory {name: 'src'})
              -[:CONTAINS]->(:Directory {name: $appName})
              <-[:HAS_ROOT]-(a:Application {name: $appName})
        RETURN a;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName, "appName", appNameOne));

    Application applicationTwo = session.queryForObject(Application.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:HAS_ROOT]->(:Directory {name: $repoName})
              -[:CONTAINS]->(:Directory {name: 'src'})
              -[:CONTAINS]->(:Directory {name: 'org'})
              -[:CONTAINS]->(:Directory {name: $appName})
              <-[:HAS_ROOT]-(a:Application {name: $appName})
        RETURN a;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName, "appName", appNameTwo));

    assertEquals("", stateData.getCommitId());
    assertNotNull(applicationOne);
    assertNotNull(applicationTwo);
  }

  @Test
  void testGetStateDataWithExistingCommits() {
    String appName = "testApp";
    String commitHash = "commit1";
    String fileHash = "1";
    String filePath = "src/File1.java";

    StateDataRequest stateDataPreparationRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).putAllApplicationPaths(Map.of(appName, "")).build();

    stateDataService.getStateData(stateDataPreparationRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    CommitData commitDataOne =
        CommitData.newBuilder().setCommitId(commitHash).setRepositoryName(repoName)
            .setBranchName(branchName).setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .addAllAddedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash(fileHash).setFilePath(filePath).build()))
            .build();

    commitService.persistCommit(commitDataOne).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Map<String, Double> testMap = Map.of("count", 1d, "lines", 2d);

    FileData fileDataOne =
        FileData.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setFileHash(fileHash).setFilePath(filePath).setLanguage(Language.JAVA)
            .addAllImportNames(List.of("Test")).addAllClasses(List.of()).addAllFunctions(List.of())
            .putAllMetrics(testMap).setLastEditor("Testi").setAddedLines(1).setModifiedLines(1)
            .setDeletedLines(0).build();

    fileDataService.persistFile(fileDataOne).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).putAllApplicationPaths(Map.of(appName, "")).build();

    StateData stateData = stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Landscape landscape = session.queryForObject(Landscape.class, """
        MATCH (l:Landscape {tokenId: $tokenId})
        RETURN l;
        """, Map.of("tokenId", landscapeToken));

    Repository repository = session.queryForObject(Repository.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(r:Repository {name: $repoName})
        RETURN r;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName));

    Directory rootDirectory = session.queryForObject(Directory.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:HAS_ROOT]->(root:Directory {name: $repoName})
        RETURN root;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName));

    Application application = session.queryForObject(Application.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:HAS_ROOT]->(:Directory {name: $repoName})
              <-[:HAS_ROOT]-(a:Application {name: $appName})
        RETURN a;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName, "appName", appName));

    Branch branch = session.queryForObject(Branch.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:CONTAINS]->(b:Branch {name: $branchName})
        RETURN b;
        """, Map.of("tokenId", landscapeToken, "repoName", repoName, "branchName", branchName));

    assertEquals(commitHash, stateData.getCommitId());
    assertNotNull(landscape);
    assertNotNull(repository);
    assertNotNull(rootDirectory);
    assertNotNull(application);
    assertNotNull(branch);
  }
}
