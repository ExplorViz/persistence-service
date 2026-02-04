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
        ORDER BY coalesce(c.commitDate, 0) ASC
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
    
    // First, verify existence and get the commit
    final Optional<Commit> existingCommit = findCommitByHashAndLandscapeToken(session, commitHash, tokenId);
    if (existingCommit.isEmpty()) {
      return Optional.empty();
    }
    
    final String hash = existingCommit.get().getHash();

    // 1. Find all Directory IDs that are parents of files in this commit
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

    // 2. Load all these directories at depth 1 to establish the parent-child relationships
    if (!dirIds.isEmpty()) {
       session.loadAll(net.explorviz.persistence.ogm.Directory.class, dirIds, 1);
    }
    
    // 3. Load the commit itself at depth 3
    // This will load Commit -> FileRevision -> Clazz -> Function
    // Since Directories are already in the session, they will be correctly linked.
    return Optional.ofNullable(session.load(Commit.class, hash, 3));
  }

  public java.util.List<Commit> findLatestDeepCommits(final String tokenId) {
    final Session session = sessionFactory.openSession();
    
    // Step 1: Find all repositories for this token
    final Iterable<Map<String, Object>> repoResult = session.query("""
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(r:Repository)
        RETURN r.name as name
        """, Map.of("tokenId", tokenId));
    
    final java.util.List<Commit> latestCommits = new java.util.ArrayList<>();
    for (final Map<String, Object> row : repoResult) {
      final String repoName = (String) row.get("name");
      
      // Step 2: Find latest commit for this repository
      final Iterable<Map<String, Object>> commitResult = session.query("""
          MATCH (l:Landscape {tokenId: $tokenId})
                -[:CONTAINS]->(r:Repository {name: $repoName})
          MATCH (r)-[:CONTAINS]->(c:Commit)
          RETURN c.hash as hash
          ORDER BY coalesce(c.commitDate, 0) DESC, c.hash DESC
          LIMIT 1
          """, Map.of("tokenId", tokenId, "repoName", repoName));
      
      if (commitResult.iterator().hasNext()) {
        final String hash = (String) commitResult.iterator().next().get("hash");
        findDeepCommit(hash, tokenId).ifPresent(latestCommits::add);
      }
    }
    
    return latestCommits;
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

  public java.util.List<String> findBranches(final String tokenId, final String repoName) {
    final Session session = sessionFactory.openSession();
    final Iterable<Map<String, Object>> result = session.query("""
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(r:Repository {name: $repoName})
              -[:CONTAINS]->(b:Branch)
        RETURN b.name as name;
        """, Map.of("tokenId", tokenId, "repoName", repoName));

    final java.util.List<String> branches = new java.util.ArrayList<>();
    result.forEach(row -> branches.add((String) row.get("name")));
    return branches;
  }

  public java.util.List<Commit> findCommitsByBranch(final String tokenId,
      final String repoName, final String branchName) {
    final Session session = sessionFactory.openSession();
    final Iterable<Commit> result = session.query(Commit.class, """
        MATCH (l:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(r:Repository {name: $repoName})
              -[:CONTAINS]->(c:Commit)
              -[:BELONGS_TO]->(b:Branch {name: $branchName})
        RETURN c
        ORDER BY coalesce(c.commitDate, 0) DESC;
        """, Map.of("tokenId", tokenId, "repoName", repoName, "branchName", branchName));

    final java.util.List<Commit> commits = new java.util.ArrayList<>();
    result.forEach(commits::add);
    return commits;
  }

  public java.util.List<Commit> findCommitsByRepository(final String tokenId,
      final String repoName) {
    final Session session = sessionFactory.openSession();
    final Iterable<Commit> result = session.query(Commit.class, """
        MATCH (l:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(r:Repository {name: $repoName})
              -[:CONTAINS]->(c:Commit)
        RETURN c
        ORDER BY coalesce(c.commitDate, 0) DESC;
        """, Map.of("tokenId", tokenId, "repoName", repoName));

    final java.util.List<Commit> commits = new java.util.ArrayList<>();
    result.forEach(commits::add);
    return commits;
  }
}
