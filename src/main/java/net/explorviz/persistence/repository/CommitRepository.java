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

  public Optional<Commit> findLatestCommitByRepositoryNameAndLandscapeTokenAndBranchName(final Session session,
      final String repoName, final String tokenId, final String branchName) {
    final String FIND_LATEST_COMMIT_BY_LANDSCAPE_AND_REPOSITORY_AND_BRANCH = """
      MATCH (l:Landscape {id: $tokenId})-[:CONTAINS]->(r:Repository {name: $repoName})
      MATCH (r)-[:CONTAINS]->(b:Branch {name: $branchName})
      MATCH (r)-[:CONTAINS]->(c:Commit)-[:BELONGS_TO]->(b)
      WHERE NOT EXISTS {
        MATCH (child:Commit)-[:HAS_PARENT]->(c)
        WHERE (child)-[:BELONGS_TO]->(b)
      }
      RETURN c
      LIMIT 1;
      """;

    return Optional.ofNullable(session.queryForObject(Commit.class,
        FIND_LATEST_COMMIT_BY_LANDSCAPE_AND_REPOSITORY_AND_BRANCH,
        Map.of("tokenId", tokenId, "repoName", repoName, "branchName", branchName)));
  }

  public Optional<Commit> findCommitByHashAndLandscapeToken(final Session session,
      final String commitHash, final String tokenId) {
    return Optional.ofNullable(
        session.queryForObject(Commit.class, """
                MATCH (:Landscape {tokenId: $tokenId})
                      -[:CONTAINS]->(:Repository)
                      -[:CONTAINS]->(c:Commit {hash: $commitHash})
                RETURN c;
                """,
            Map.of("tokenId", tokenId, "commitHash", commitHash)));
  }

  public Optional<Commit> findCommitByHashAndLandscapeToken(
      final String commitHash, final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findCommitByHashAndLandscapeToken(session, commitHash, tokenId);
  }

  public Commit getOrCreateCommit(final Session session, final String commitHash,
      final String tokenId) {
    return findCommitByHashAndLandscapeToken(session, commitHash, tokenId).orElse(
        new Commit(commitHash));
  }
}
