package net.explorviz.persistence.repository;

import com.google.common.collect.Lists;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
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
   * Find latest commit for which we have seen a CommitData message and also a FileData message for
   * every file included in the commit.
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

  /**
   * Returns every commit (along with its branch) in the repository for a given application.
   */
  public List<Commit> findCommitsWithBranchForApplicationAndLandscapeToken(final Session session,
      final String landscapeToken, final String applicationName) {
    return Lists.newArrayList(session.query(Commit.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(repo:Repository)
              -[:HAS_ROOT]->(:Directory)
              -[:CONTAINS]->*(:Directory)<-[:HAS_ROOT]-(:Application {name: $appName})
        MATCH (repo)-[:CONTAINS]->(c:Commit)
        OPTIONAL MATCH (c)-[r:BELONGS_TO]->(b:Branch)
        OPTIONAL MATCH (c)-[h:HAS_PARENT]->()
        RETURN DISTINCT c, r, b, h;
        """, Map.of("tokenId", landscapeToken, "appName", applicationName)));
  }

  public Commit getOrCreateCommit(final Session session, final String commitHash,
      final String tokenId) {
    return findCommitByHashAndLandscapeToken(session, commitHash, tokenId).orElse(
        new Commit(commitHash));
  }
}
