package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Commit;
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
    final Optional<Commit> commit = findCommitByHashAndLandscapeToken(session, commitHash, tokenId);
    if (commit.isEmpty()) {
      return Optional.empty();
    }
    // Load with depth 3 to get FileRevisions(1), Classes(2), Functions(3)
    return Optional.ofNullable(session.load(Commit.class, commit.get().getHash(), 3));
  }

  public Optional<Commit> findLatestDeepCommit(final String tokenId) {
    final Session session = sessionFactory.openSession();
    // Find latest commit in landscape
    final Optional<Commit> latest = Optional.ofNullable(session.queryForObject(Commit.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository)
              -[:CONTAINS]->(c:Commit)
        WITH c
        ORDER BY c.commitDate DESC
        LIMIT 1
        RETURN c;
        """, Map.of("tokenId", tokenId)));

    if (latest.isEmpty()) {
      return Optional.empty();
    }
    
    // Load deep
    return Optional.ofNullable(session.load(Commit.class, latest.get().getHash(), 3));
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
