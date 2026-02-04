package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.repository.CommitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class CommitRepositoryTest {

  @Inject
  CommitRepository commitRepository;

  @Inject
  SessionFactory sessionFactory;

  @BeforeEach
  void cleanup() {
    Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  @Test
  void testFindLatestCommit() {
    Session session = sessionFactory.openSession();

    FileRevision file1 = new FileRevision("File1.java");
    file1.setHasFileData(true);

    FileRevision file2 = new FileRevision("File2.java");

    Branch branch = new Branch("main");

    Commit commit1 = new Commit("commit1");
    commit1.addFileRevision(file1);
    commit1.setCommitDate(Instant.ofEpochMilli(1000));
    commit1.setBranch(branch);

    Commit commit2 = new Commit("commit2");
    commit2.addParent(commit1);
    commit2.addFileRevision(file1);
    commit2.addFileRevision(file2);
    commit2.setCommitDate(Instant.ofEpochMilli(2000));
    commit2.setBranch(branch);

    Repository repository = new Repository("myrepo");
    repository.addBranch(branch);
    repository.addCommit(commit1);
    repository.addCommit(commit2);

    Landscape landscape = new Landscape("mytokenvalue");
    landscape.addRepository(repository);

    session.save(landscape);

    Optional<Commit> latestCommit = commitRepository.findLatestFullyPersistedCommit(
        session, "myrepo", "mytokenvalue", "main");

    assertTrue(latestCommit.isPresent());
    assertEquals("commit1", latestCommit.get().getHash());
  }

  @Test
  void testFindLatestDeepCommits() {
    Session session = sessionFactory.openSession();

    // Repo 1
    Commit c1 = new Commit("c1");
    c1.setCommitDate(Instant.ofEpochMilli(1000));
    Commit c2 = new Commit("c2");
    c2.setCommitDate(Instant.ofEpochMilli(2000));
    Repository r1 = new Repository("repo1");
    r1.addCommit(c1);
    r1.addCommit(c2);

    // Repo 2
    Commit c3 = new Commit("c3");
    c3.setCommitDate(Instant.ofEpochMilli(3000));
    Repository r2 = new Repository("repo2");
    r2.addCommit(c3);

    Landscape landscape = new Landscape("token123");
    landscape.addRepository(r1);
    landscape.addRepository(r2);

    session.save(landscape);

    java.util.List<Commit> commits = commitRepository.findLatestDeepCommits("token123");

    assertEquals(2, commits.size());
    // Should contain c2 (latest for r1) and c3 (latest for r2)
    // We can check hashes
    java.util.List<String> hashes = commits.stream().map(Commit::getHash).collect(java.util.stream.Collectors.toList());
    assertTrue(hashes.contains("c2"));
    assertTrue(hashes.contains("c3"));
    org.junit.jupiter.api.Assertions.assertFalse(hashes.contains("c1"));
  }

  @Test
  void testFindDeepDirectoryStructure() {
    Session session = sessionFactory.openSession();

    // Create deep directory structure: root -> level1 -> level2 -> level3 -> file
    net.explorviz.persistence.ogm.Directory root = new net.explorviz.persistence.ogm.Directory("root");
    net.explorviz.persistence.ogm.Directory level1 = new net.explorviz.persistence.ogm.Directory("level1");
    net.explorviz.persistence.ogm.Directory level2 = new net.explorviz.persistence.ogm.Directory("level2");
    net.explorviz.persistence.ogm.Directory level3 = new net.explorviz.persistence.ogm.Directory("level3");
    
    // Wire up parent/child relations
    // Note: Directory.subdirectories is OUTGOING CONTAINS
    // Directory.parent is INCOMING CONTAINS
    // Setting one side should be enough if using simple setters, but relations need care.
    // Use the side that owns the relationship or just save them.
    // Directory entity has fields for both but OGM handles the graph.
    // Let's rely on saving the graph.

    // root -> level1
    root.addSubdirectory(level1);
    
    // level1 -> level2
    level1.addSubdirectory(level2);
    
    // level2 -> level3
    level2.addSubdirectory(level3);

    FileRevision file = new FileRevision("DeepFile.java");
    file.setHash("hash1");
    file.setParentDirectory(level3);
    
    // level3 -> file
    // level3.addFileRevision(file); // Removed method
    
    Commit c = new Commit("deep-commit");
    c.addFileRevision(file);
    
    Repository r = new Repository("deep-repo");
    r.addCommit(c);
    
    Landscape l = new Landscape("deep-token");
    l.addRepository(r);
    
    // Save everything. Saving landscape should cascade if configured, but let's be safe.
    // Directory structure needs to be saved.
    // Commit -> File is cascaded? Commit has @Relationship...
    // Directory -> Subdir?
    // Let's save individually or bottom-up to be safe in test.
    session.save(l);
    session.save(r);
    session.save(c);
    session.save(file);
    session.save(level3);
    session.save(level2);
    session.save(level1);
    session.save(root);
    
    // Clear session to force reload
    session.clear();
    
    Optional<Commit> fetchedCommit = commitRepository.findDeepCommit("deep-commit", "deep-token");
    
    assertTrue(fetchedCommit.isPresent());
    Commit loadedCommit = fetchedCommit.get();
    assertEquals(1, loadedCommit.getFileRevisions().size());
    FileRevision loadedFile = loadedCommit.getFileRevisions().iterator().next();
    
    // Check hierarchy
    net.explorviz.persistence.ogm.Directory d3 = loadedFile.getParentDirectory();
    org.junit.jupiter.api.Assertions.assertNotNull(d3, "Level 3 should be loaded");
    assertEquals("level3", d3.getName());
    
    net.explorviz.persistence.ogm.Directory d2 = d3.getParent();
    org.junit.jupiter.api.Assertions.assertNotNull(d2, "Level 2 should be loaded");
    assertEquals("level2", d2.getName());
    
    net.explorviz.persistence.ogm.Directory d1 = d2.getParent();
    org.junit.jupiter.api.Assertions.assertNotNull(d1, "Level 1 should be loaded");
    assertEquals("level1", d1.getName());
    
    net.explorviz.persistence.ogm.Directory dRoot = d1.getParent();
    org.junit.jupiter.api.Assertions.assertNotNull(dRoot, "Root should be loaded");
    assertEquals("root", dRoot.getName());
  }
}
