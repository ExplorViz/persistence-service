package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.Timestamp;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.ogm.FileRevision;
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
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class FileDataServiceTest {

  private static final long GRPC_AWAIT_SECONDS = 5;

  @GrpcClient
  CommitService commitService;

  @GrpcClient
  StateDataService stateDataService;

  @GrpcClient
  FileDataService fileDataService;

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

    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setBranchName(branchName).build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));
  }

  @Test
  void testPersistFileWithCorrectMetrics() {
    Session session = sessionFactory.openSession();

    String commitHash = "commit1";
    String filePath = "myrepo/src/File1.java";
    String fileHash = "1";

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

    FileRevision file = session.queryForObject(FileRevision.class, """
        MATCH (f:FileRevision {hash: $fileHash})
        RETURN f;
        """, Map.of("fileHash", fileHash));

    for (String k : file.getMetrics().keySet()) {
      assertEquals(testMap.get(k), file.getMetrics().get(k));
    }

  }

  @Test
  void testPersistFileDuplicateFileOnDifferentPaths() {
    Session session = sessionFactory.openSession();

    String commitHash = "commit1";
    String filePathOne = "myrepo/src/File1.java";
    String filePathTwo = "myrepo/src/hollandaise/File1.java";
    String fileHash = "1";

    CommitData commitDataOne =
        CommitData.newBuilder().setCommitId(commitHash).setRepositoryName(repoName)
            .setBranchName(branchName).setLandscapeToken(landscapeToken)
            .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
            .addAllAddedFiles(List.of(
                FileIdentifier.newBuilder().setFileHash(fileHash).setFilePath(filePathOne).build(),
                FileIdentifier.newBuilder().setFileHash(fileHash).setFilePath(filePathTwo).build()))
            .build();

    commitService.persistCommit(commitDataOne).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Map<String, Double> testMap = Map.of("count", 1d, "lines", 2d);

    FileData fileDataOne =
        FileData.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setFileHash(fileHash).setFilePath(filePathTwo).setLanguage(Language.JAVA)
            .addAllImportNames(List.of("Test")).addAllClasses(List.of()).addAllFunctions(List.of())
            .putAllMetrics(testMap).setLastEditor("Testi").setAddedLines(1).setModifiedLines(1)
            .setDeletedLines(0).build();

    FileData fileDataTwo =
        FileData.newBuilder().setLandscapeToken(landscapeToken).setRepositoryName(repoName)
            .setFileHash(fileHash).setFilePath(filePathTwo).setLanguage(Language.JAVA)
            .addAllImportNames(List.of("Test")).addAllClasses(List.of()).addAllFunctions(List.of())
            .putAllMetrics(testMap).setLastEditor("Testi").setAddedLines(1).setModifiedLines(1)
            .setDeletedLines(0).build();

    fileDataService.persistFile(fileDataOne).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));
    fileDataService.persistFile(fileDataTwo).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Iterable<FileRevision> files = session.query(FileRevision.class, """
        MATCH (f:FileRevision {hash: $fileHash})
        RETURN f;
        """, Map.of("fileHash", fileHash));

    List<FileRevision> fileList = new ArrayList<>();
    files.forEach(fileList::add);

    assertEquals(2, fileList.size());
  }

}
