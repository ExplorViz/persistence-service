package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.FileRevision;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class CommitRepository {

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Commit> findCommitById(final Session session, final String commitHash) {
    return Optional.ofNullable(
        session.queryForObject(Commit.class, "MATCH (c:Commit {hash: $hash}) RETURN c;",
            Map.of("hash", commitHash)));
  }

  public Optional<Commit> findCommitById(final String commitHash) {
    final Session session = sessionFactory.openSession();
    return findCommitById(session, commitHash);
  }

  /**
   * Find latest commit for which we have seen a CommitData message and also a FileData message
   * for every file included in the commit.
   */
  public Optional<Commit> findLatestFullyPersistedCommit(
      final Session session, final String repoName, final String tokenId, final String branchName) {
    return Optional.ofNullable(session.queryForObject(Commit.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:CONTAINS]->(c:Commit)
              -[:BELONGS_TO]->(:Branch {name: $branchName})
        WITH c, [(c)-[:CONTAINS]->(f:FileRevision) | f] AS filesInCommit
        WHERE all(file IN filesInCommit WHERE file.hasFileData)
              AND NOT isEmpty(filesInCommit)
        RETURN c
        ORDER BY c.commitDate
        LIMIT 1;""", Map.of("tokenId", tokenId, "repoName", repoName, "branchName", branchName)));
  }

  public Optional<Commit> findCommitByHashAndLandscapeToken(final Session session,
      final String commitHash, final String tokenId) {
    return Optional.ofNullable(session.queryForObject(Commit.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository)
              -[:CONTAINS]->(c:Commit {hash: $commitHash})
        RETURN c;
        """, Map.of("tokenId", tokenId, "commitHash", commitHash)));
  }

  public Optional<Commit> findCommitByHashAndLandscapeToken(final String commitHash,
      final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findCommitByHashAndLandscapeToken(session, commitHash, tokenId);
  }

  public Commit getOrCreateCommit(final Session session, final String commitHash,
      final String tokenId) {
    return findCommitByHashAndLandscapeToken(session, commitHash, tokenId).orElse(
        new Commit(commitHash));
  }

  public Optional<Commit> findDeepCommit(final String commitHash, final String tokenId) {
    final Session session = sessionFactory.openSession();
    // Verify existence in landscape first
    final Optional<Commit> existingCommit = findCommitByHashAndLandscapeToken(session, commitHash, tokenId);
    if (existingCommit.isEmpty()) {
      return Optional.empty();
    }
    
    // Load deep
    // We use a two-step loading process to avoid traversing the entire commit history
    // (which would happen if we used a large depth on Commit) while still retrieving
    // the deep directory structure (which requires a large depth on FileRevision).
    
    // Load deep
    // We use a multi-step loading process to ensure the graph is fully hydrated without fetching excessive history.
    
    final String hash = existingCommit.get().getHash();

    // 1. Get Directory IDs via Cypher
    // We need to find all directories in the path from files to root
    Iterable<Map<String, Object>> dirResult = session.query("""
       MATCH (c:Commit {hash: $hash})-[:CONTAINS]->(f:FileRevision)
       MATCH path = (d:Directory)-[:CONTAINS*]->(f)
       UNWIND nodes(path) as dirNode
       WITH DISTINCT dirNode
       WHERE dirNode:Directory
       RETURN id(dirNode) as id
    """, Map.of("hash", hash));

    java.util.List<Long> dirIds = new java.util.ArrayList<>();
    dirResult.forEach(row -> dirIds.add((Long) row.get("id")));

    // 2. Get FileRevision IDs via Cypher
    Iterable<Map<String, Object>> fileResult = session.query("""
       MATCH (c:Commit {hash: $hash})-[:CONTAINS]->(f:FileRevision)
       RETURN id(f) as id
    """, Map.of("hash", hash));
    
    java.util.List<Long> fileIds = new java.util.ArrayList<>();
    fileResult.forEach(row -> fileIds.add((Long) row.get("id")));

    // Clear session to ensure clean slate
    session.clear();

    // 3. Load Directories (depth 1 links parents)
    if (!dirIds.isEmpty()) {
       session.loadAll(net.explorviz.persistence.ogm.Directory.class, dirIds, 1);
    }
    
    // 4. Load Files (depth 1 links to Directories and Classes)
    if (!fileIds.isEmpty()) {
       session.loadAll(FileRevision.class, fileIds, 1);
    }
    
    // 5. Load Commit (depth 1 links to Files)
    final Commit commit = session.load(Commit.class, hash, 1);
    
    if (commit == null) {
      return Optional.empty();
    }
    
    return Optional.of(commit);
  }

  public java.util.List<Commit> findLatestDeepCommits(final String tokenId) {
    final Session session = sessionFactory.openSession();
    // Find latest commit for each repository in landscape
    final Iterable<Map<String, Object>> result = session.query("""
        MATCH (:Landscape {tokenId: $tokenId})-[:CONTAINS]->(r:Repository)
        MATCH (r)-[:CONTAINS]->(c:Commit)
        WITH r, c
        ORDER BY c.commitDate DESC
        WITH r, head(collect(c)) as latestCommit
        RETURN latestCommit.hash as hash
        """, Map.of("tokenId", tokenId));

    final java.util.List<Commit> commits = new java.util.ArrayList<>();
    
    for (final Map<String, Object> row : result) {
      final String hash = (String) row.get("hash");
      
      // Load Dirs - capture all directories in the path
      Iterable<Map<String, Object>> dirResult = session.query("""
         MATCH (c:Commit {hash: $hash})-[:CONTAINS]->(f:FileRevision)
         MATCH path = (d:Directory)-[:CONTAINS*]->(f)
         UNWIND nodes(path) as dirNode
         WITH DISTINCT dirNode
         WHERE dirNode:Directory
         RETURN id(dirNode) as id
      """, Map.of("hash", hash));

      java.util.List<Long> dirIds = new java.util.ArrayList<>();
      dirResult.forEach(dirRow -> dirIds.add((Long) dirRow.get("id")));
      
      if (!dirIds.isEmpty()) {
         session.loadAll(net.explorviz.persistence.ogm.Directory.class, dirIds, 1);
      }
      
      // Load Files
      Iterable<Map<String, Object>> fileResult = session.query("""
         MATCH (c:Commit {hash: $hash})-[:CONTAINS]->(f:FileRevision)
         RETURN id(f) as id
      """, Map.of("hash", hash));

      java.util.List<Long> fileIds = new java.util.ArrayList<>();
      fileResult.forEach(fileRow -> fileIds.add((Long) fileRow.get("id")));
      
      if (!fileIds.isEmpty()) {
         session.loadAll(FileRevision.class, fileIds, 1);
      }
      
      // Load commit
      final Commit c = session.load(Commit.class, hash, 1);
      if (c != null) {
        commits.add(c);
      }
    }
    
    return commits;
  }

  public Optional<String> findRepositoryName(final String tokenId, final String commitHash) {
    final Session session = sessionFactory.openSession();
    final Iterable<Map<String, Object>> result = session.query("""
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(r:Repository)
              -[:CONTAINS]->(c:Commit {hash: $commitHash})
        RETURN r.name as name;
        """, Map.of("tokenId", tokenId, "commitHash", commitHash));

    if (result.iterator().hasNext()) {
      return Optional.ofNullable((String) result.iterator().next().get("name"));
    }
    return Optional.empty();
  }
}
