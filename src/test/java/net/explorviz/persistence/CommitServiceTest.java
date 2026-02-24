package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.Timestamp;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.FileRevision;
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
  void testFilesAddedFromCommitData() {
    String commitHash = "commit1";
    String fileHashOne = "1";
    String fileNameOne = "File1.java";
    String filePathOne = "src/" + fileNameOne;
    String fileHashTwo = "2";
    String fileNameTwo = "File2.java";
    String filePathTwo = "src/" + fileNameTwo;

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitData1 =
        CommitData.newBuilder().setCommitId(commitHash).setRepositoryName(repoName)
            .setBranchName(branchName).setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .addAllAddedFiles(List.of(FileIdentifier.newBuilder().setFileHash(fileHashOne)
                    .setFilePath(filePathOne).build(),
                FileIdentifier.newBuilder().setFileHash(fileHashTwo)
                    .setFilePath(filePathTwo).build())).build();

    commitService.persistCommit(commitData1).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    Commit commit = session.queryForObject(Commit.class, """
        MATCH (c:Commit {hash: $commitHash}) RETURN c;
        """, Map.of("commitHash", commitHash));

    Iterable<FileRevision> files = session.query(FileRevision.class, """
            MATCH (:Landscape {tokenId: $landscapeToken})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:CONTAINS]->(:Commit {hash: $commitHash})
              -[:CONTAINS]->(f:FileRevision)
            RETURN f
            ORDER BY f.name ASC;
            """,
        Map.of("landscapeToken", landscapeToken, "repoName", repoName, "commitHash", commitHash));

    Boolean correctRepoPath = session.queryForObject(Boolean.class, """
        RETURN EXISTS {
        MATCH (:Landscape {tokenId: $landscapeToken})
          -[:CONTAINS]->(:Repository {name: $repoName})
          -[:HAS_ROOT]->(:Directory {name: $repoName})
          -[:CONTAINS]->(:Directory {name: $srcName})
          -[:CONTAINS]->(:FileRevision {name: $fileNameOne})
        
        MATCH (:Landscape {tokenId: $landscapeToken})
          -[:CONTAINS]->(:Repository {name: $repoName})
          -[:HAS_ROOT]->(:Directory {name: $repoName})
          -[:CONTAINS]->(:Directory {name: $srcName})
          -[:CONTAINS]->(:FileRevision {name: $fileNameTwo})
        } AS exists
        """, Map.of("landscapeToken", landscapeToken, "repoName", repoName, "srcName", "src",
        "fileNameOne", fileNameOne, "fileNameTwo", fileNameTwo));

    int count = 0;
    for (FileRevision file : files) {
      count++;
    }

    Iterator<FileRevision> it = files.iterator();

    assertEquals(commitHash, commit.getHash());
    assertEquals(2, count);
    assertEquals(fileNameOne, it.next().getName());
    assertEquals(fileNameTwo, it.next().getName());
    assertTrue(correctRepoPath);
  }
}
