package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Iterators;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Tag;
import net.explorviz.persistence.proto.CommitData;
import net.explorviz.persistence.proto.CommitService;
import net.explorviz.persistence.proto.FileData;
import net.explorviz.persistence.proto.FileDataService;
import net.explorviz.persistence.proto.FileIdentifier;
import net.explorviz.persistence.proto.Language;
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
  void testPersistCommit() {
    String commitHash = "commit1";
    String fileHashOne = "1";
    String fileNameOne = "File1.java";
    String filePathOne = "src/" + fileNameOne;
    String fileHashTwo = "2";
    String fileNameTwo = "File2.java";
    String filePathTwo = "src/" + fileNameTwo;
    String tagName = "tag";

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitDataOne =
        CommitData.newBuilder().setCommitId(commitHash).setRepositoryName(repoName)
            .setBranchName(branchName).setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .setCommitDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .addAllTags(List.of(tagName)).addAllAddedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash(fileHashOne).setFilePath(filePathOne).build(),
                FileIdentifier.newBuilder().setFileHash(fileHashTwo).setFilePath(filePathTwo).build()))
            .build();

    commitService.persistCommit(commitDataOne).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    Commit commit = session.queryForObject(Commit.class, """
            MATCH (:Landscape {tokenId: $landscapeToken})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:CONTAINS]->(c:Commit {hash: $commitHash})
              -[:BELONGS_TO]->(:Branch {name: $branchName})
            RETURN c;
            """,
        Map.of("landscapeToken", landscapeToken, "repoName", repoName, "commitHash", commitHash,
            "branchName", branchName));

    Tag tag = session.queryForObject(Tag.class, """
            MATCH (:Landscape {tokenId: $landscapeToken})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:CONTAINS]->(:Commit {hash: $commitHash})
              -[:IS_TAGGED_WITH]->(t:Tag {name: $tagName})
            RETURN t;
            """,
        Map.of("landscapeToken", landscapeToken, "repoName", repoName, "commitHash", commitHash,
            "tagName", tagName));

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
              -[:CONTAINS]->(src:Directory {name: 'src'})
            
            MATCH (src)-[:CONTAINS]->(:FileRevision {name: $fileNameOne, hash: $fileHashOne})
            MATCH (src)-[:CONTAINS]->(:FileRevision {name: $fileNameTwo, hash: $fileHashTwo})
            } AS exists
            """,
        Map.of("landscapeToken", landscapeToken, "repoName", repoName, "fileNameOne", fileNameOne,
            "fileHashOne", fileHashOne, "fileNameTwo", fileNameTwo, "fileHashTwo", fileHashTwo));

    int count = 0;
    for (FileRevision file : files) {
      count++;
    }
    Iterator<FileRevision> it = files.iterator();

    assertNotNull(commit);
    assertNotNull(tag);
    assertEquals(2, count);
    assertEquals(fileNameOne, it.next().getName());
    assertEquals(fileNameTwo, it.next().getName());
    assertTrue(correctRepoPath);
  }

  @Test
  void testPersistCommitWithParentCommit() {
    String commitHashOne = "commit1";
    String commitHashTwo = "commit2";

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitDataOne =
        CommitData.newBuilder().setCommitId(commitHashOne).setRepositoryName(repoName)
            .setBranchName(branchName).setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .setCommitDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build()).build();

    commitService.persistCommit(commitDataOne).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitDataTwo =
        CommitData.newBuilder().setCommitId(commitHashTwo).setRepositoryName(repoName)
            .setParentCommitId(commitHashOne).setBranchName(branchName)
            .setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(30).setNanos(100).build())
            .setCommitDate(Timestamp.newBuilder().setSeconds(30).setNanos(100).build()).build();

    commitService.persistCommit(commitDataTwo).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    Boolean correctDatabase = session.queryForObject(Boolean.class, """
        RETURN EXISTS {
        MATCH (:Landscape {tokenId: $landscapeToken})
          -[:CONTAINS]->(r:Repository {name: $repoName})
          -[:CONTAINS]->(c1:Commit {hash: $commitHashOne})
        
        MATCH (r)
          -[:CONTAINS]->(:Commit {hash: $commitHashTwo})
          -[:HAS_PARENT]->(c1)
        } AS exists
        """, Map.of("landscapeToken", landscapeToken, "repoName", repoName, "commitHashOne",
        commitHashOne, "commitHashTwo", commitHashTwo));

    assertTrue(correctDatabase);
  }

  @Test
  void testPersistCommitAddedModifiedDeletedUnchangedFiles() throws InterruptedException {
    String commitHashOne = "commit1";
    String commitHashTwo = "commit2";
    String fileHashOne = "1";
    String fileHashOneMod = "11";
    String fileNameOne = "File1.java";
    String filePathOne = "src/" + fileNameOne;
    String fileHashTwo = "2";
    String fileNameTwo = "File2.java";
    String filePathTwo = "src/" + fileNameTwo;
    String fileHashThree = "3";
    String fileNameThree = "File3.java";
    String filePathThree = "src/" + fileNameThree;
    String fileHashDel = "4";
    String fileNameDel = "FileDel.java";
    String filePathDel = "src/" + fileNameDel;
    String tagName = "tag";

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitDataOne =
        CommitData.newBuilder().setCommitId(commitHashOne).setRepositoryName(repoName)
            .setBranchName(branchName).setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .setCommitDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .addAllTags(List.of(tagName)).addAllAddedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash(fileHashOne).setFilePath(filePathOne).build(),
                FileIdentifier.newBuilder().setFileHash(fileHashTwo).setFilePath(filePathTwo).build(),
                FileIdentifier.newBuilder().setFileHash(fileHashDel).setFilePath(filePathDel).build()))
            .build();

    commitService.persistCommit(commitDataOne).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitDataTwo =
        CommitData.newBuilder().setCommitId(commitHashTwo).setRepositoryName(repoName)
            .setParentCommitId(commitHashOne).setBranchName(branchName)
            .setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(30).setNanos(100).build())
            .setCommitDate(Timestamp.newBuilder().setSeconds(30).setNanos(100).build())
            .addAllTags(List.of(tagName)).addAllAddedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash(fileHashThree).setFilePath(filePathThree)
                    .build())).addAllModifiedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash(fileHashOneMod).setFilePath(filePathOne)
                    .build())).addAllUnchangedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash(fileHashTwo).setFilePath(filePathTwo).build()))
            .build();

    commitService.persistCommit(commitDataTwo).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    Map<String, Object> params = new HashMap<>();
    params.put("landscapeToken", landscapeToken);
    params.put("repoName", repoName);
    params.put("fileNameOne", fileNameOne);
    params.put("fileHashOne", fileHashOne);
    params.put("fileNameTwo", fileNameTwo);
    params.put("fileHashTwo", fileHashTwo);
    params.put("fileNameThree", fileNameThree);
    params.put("fileHashThree", fileHashThree);
    params.put("fileHashOneMod", fileHashOneMod);
    params.put("fileNameDel", fileNameDel);
    params.put("fileHashDel", fileHashDel);
    params.put("commitHashOne", commitHashOne);
    params.put("commitHashTwo", commitHashTwo);
    params.put("tagName", tagName);
    params.put("branchName", branchName);

    Boolean databaseIsCorrect = session.queryForObject(Boolean.class, """
        RETURN EXISTS {
        MATCH (:Landscape {tokenId: $landscapeToken})
          -[:CONTAINS]->(r:Repository {name: $repoName})
          -[:HAS_ROOT]->(:Directory {name: $repoName})
          -[:CONTAINS]->(src:Directory {name: 'src'})
        
        MATCH (r)-[:CONTAINS]->(c1:Commit {hash: $commitHashOne})
        MATCH (c1)-[:CONTAINS]->(f1:FileRevision {name: $fileNameOne, hash: $fileHashOne})
        MATCH (src)-[:CONTAINS]->(f1)
        MATCH (c1)-[:CONTAINS]->(f2:FileRevision {name: $fileNameTwo, hash: $fileHashTwo})
        MATCH (src)-[:CONTAINS]->(f2)
        MATCH (c1)-[:CONTAINS]->(f_del:FileRevision {name: $fileNameDel, hash: $fileHashDel})
        MATCH (src)-[:CONTAINS]->(f_del)
        MATCH (c1)-[:IS_TAGGED_WITH]->(t:Tag {name: $tagName})
        MATCH (c1)-[:BELONGS_TO]->(branch:Branch {name: $branchName})
        
        MATCH (r)-[:CONTAINS]->(c2:Commit {hash: $commitHashTwo})
        MATCH (c2)-[:CONTAINS]->(f3:FileRevision {name: $fileNameThree, hash: $fileHashThree})
        MATCH (src)-[:CONTAINS]->(f3)
        MATCH (c2)-[:CONTAINS]->(f1_mod:FileRevision {name: $fileNameOne, hash: $fileHashOneMod})
        MATCH (src)-[:CONTAINS]->(f1_mod)
        MATCH (c2)-[:IS_TAGGED_WITH]->(t:Tag {name: $tagName})
        MATCH (c2)-[:CONTAINS]->(f2:FileRevision {name: $fileNameTwo, hash: $fileHashTwo})
        MATCH (c2)-[:BELONGS_TO]->(branch)
        
        WHERE NOT EXISTS {
          MATCH (c2)-[:CONTAINS]->(f_del)
        }
        } AS exists
        """, params);

    Integer dbSize = session.queryForObject(Integer.class, """
        MATCH (n) RETURN count(n);
        """, Map.of());

    assertEquals(13, dbSize);
    assertTrue(databaseIsCorrect);
  }

  @Test
  void testPersistCommitWithUnknownRepo() {
    String wrongRepoName = "wrong_repo";
    String commitHash = "commit1";

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    CommitData commitDataOne =
        CommitData.newBuilder().setCommitId(commitHash).setRepositoryName(wrongRepoName)
            .setBranchName(branchName).setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .setCommitDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build()).build();

    StatusRuntimeException ex = assertThrows(StatusRuntimeException.class,
        () -> commitService.persistCommit(commitDataOne).await()
            .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS)));

    assertEquals(Status.FAILED_PRECONDITION.getCode(), ex.getStatus().getCode());

    assertEquals("No corresponding state data was sent before.", ex.getStatus().getDescription());

  }
}
