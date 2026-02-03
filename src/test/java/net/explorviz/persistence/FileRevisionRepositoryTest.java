package net.explorviz.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.proto.FileIdentifier;
import net.explorviz.persistence.repository.FileRevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
public class FileRevisionRepositoryTest {
  @Inject
  FileRevisionRepository fileRevisionRepository;

  @Inject
  SessionFactory sessionFactory;

  @BeforeEach
  void cleanup() {
    Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  @Test
  void testFindNoNonExistentFileByHash() {
    Session session = sessionFactory.openSession();

    FileRevision file =
        fileRevisionRepository.getFileRevisionFromHash(session, "hash1", "testRepo", "testToken")
            .orElse(null);

    assertNull(file);
  }

  @Test
  void testFindFileByHash() throws InterruptedException {
    Session session = sessionFactory.openSession();

    String fileName = "file1";
    String fileHash = "hash1";
    String token = "testToken";
    String repoName = "testRepo";
    String commitHash = "commit1";

    FileRevision file = new FileRevision(fileHash, fileName);

    Landscape landscape = new Landscape(token);
    Repository repo = new Repository(repoName);
    Commit commit = new Commit(commitHash);
    commit.addFileRevision(file);
    repo.addCommit(commit);
    landscape.addRepository(repo);

    session.save(landscape);

    FileRevision foundFile =
        fileRevisionRepository.getFileRevisionFromHash(session, fileHash, repoName, token)
            .orElse(null);

    assertNotNull(foundFile);
  }

  @Test
  void testCreateFileStructureFromStaticDataForNewFile() {
    Session session = sessionFactory.openSession();

    String fileName = "file1";
    String fileHash = "hash1";
    String token = "testToken";
    String repoName = "testRepo";
    String commitHash = "commit1";

    Landscape landscape = new Landscape(token);
    Directory dir = new Directory(repoName);
    Repository repo = new Repository(repoName);
    repo.addRootDirectory(dir);
    Commit commit = new Commit(commitHash);
    repo.addCommit(commit);
    landscape.addRepository(repo);

    session.save(landscape);

    FileIdentifier fileIdentifier =
        FileIdentifier.newBuilder().setFileHash(fileHash).setFilePath(fileName).build();

    FileRevision file =
        fileRevisionRepository.createFileStructureFromStaticData(session, fileIdentifier, repoName,
            token, commit);

    Result result =
        session.query("MATCH (f:FileRevision {hash: $fileHash}) RETURN COUNT(f) as res;",
            Map.of("fileHash", fileHash));

    assertNotNull(file);
    assertEquals(1L, result.queryResults().iterator().next().get("res"));
  }

  @Test
  void testCreateFileStructureFromStaticDataForAlreadyExistingFile() throws InterruptedException {
    Session session = sessionFactory.openSession();

    String fileName = "file1";
    String fileHash = "hash1";
    String token = "testToken";
    String repoName = "testRepo";
    String commitHash = "commit1";

    Landscape landscape = new Landscape(token);
    Directory dir = new Directory(repoName);
    Repository repo = new Repository(repoName);
    repo.addRootDirectory(dir);
    Commit commit = new Commit(commitHash);
    repo.addCommit(commit);
    landscape.addRepository(repo);

    session.save(landscape);

    FileIdentifier fileIdentifier =
        FileIdentifier.newBuilder().setFileHash(fileHash).setFilePath(fileName).build();

    FileRevision file =
        fileRevisionRepository.createFileStructureFromStaticData(session, fileIdentifier, repoName,
            token, commit);

    FileRevision foundFile =
        fileRevisionRepository.getFileRevisionFromHash(session, fileHash, repoName, token)
            .orElse(null);

    FileRevision file2 =
        fileRevisionRepository.createFileStructureFromStaticData(session, fileIdentifier, repoName,
            token, commit);

    Result result =
        session.query("MATCH (f:FileRevision {hash: $fileHash}) RETURN COUNT(f) as res;",
            Map.of("fileHash", fileHash));

    assertNotNull(file);
    assertNotNull(foundFile);
    assertEquals(file.getId(), foundFile.getId());
    assertNotNull(file2);
    assertEquals(file.getId(), file2.getId());
    assertEquals(1L, result.queryResults().iterator().next().get("res"));
  }
}
