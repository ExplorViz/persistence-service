package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Repository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;


@ApplicationScoped
public class RepositoryRepository {

  private static final String FIND_BY_NAME_AND_LANDSCAPE_TOKEN_STATEMENT = """
      MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(r:Repository {name: $name})
        -[h:HAS_ROOT]->(root:Directory)
      RETURN r, root, h;
      """;

  private static final String FIND_REPO_WITH_FULL_FILE_TREE_OF_COMMIT = """
      MATCH (:Landscape {tokenId: $tokenId})
            -[:CONTAINS]->(r:Repository)
            -[:CONTAINS]->(c:Commit {hash: $commitHash})
      OPTIONAL MATCH (c)-[:CONTAINS]->(f:FileRevision)
      OPTIONAL MATCH (f)-[:CONTAINS]->(fn:Function)-->*(fnNode)
      OPTIONAL MATCH (f)-[:CONTAINS]->(cl:Clazz)-->*(clNode)
      OPTIONAL MATCH p = (r)-[h:HAS_ROOT]->(:Directory)-[:CONTAINS]->*(f:FileRevision)
                         <-[:CONTAINS]-(c)
      UNWIND nodes(p) AS pathNode
      UNWIND relationships(p) AS rel
      RETURN DISTINCT r, rel, pathNode, fn, fnNode, cl, clNode;
      """;

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Repository> findRepositoryByNameAndLandscapeToken(final Session session,
      final String name, final String tokenId) {
    return Optional.ofNullable(
        session.queryForObject(Repository.class, FIND_BY_NAME_AND_LANDSCAPE_TOKEN_STATEMENT,
            Map.of("tokenId", tokenId, "name", name)));
  }

  public Optional<Repository> findRepositoryByNameAndLandscapeToken(final String name,
      final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findRepositoryByNameAndLandscapeToken(session, name, tokenId);
  }

  /**
   * Returns a fully hydrated Repository object with respect to the file structure of a particular
   * commit, meaning all files and corresponding directories for the commit are fetched. This
   * includes fully hydrated functions and classes. Empty if no Repository is matched.
   */
  public Optional<Repository> getFullyHydratedRepositoryForCommit(final Session session,
      final String landscapeToken, final String commitHash) {
    return Optional.ofNullable(
        session.queryForObject(Repository.class, FIND_REPO_WITH_FULL_FILE_TREE_OF_COMMIT,
            Map.of("tokenId", landscapeToken, "commitHash", commitHash)));
  }

  public Repository getOrCreateRepository(final Session session, final String repoName,
      final String tokenId) {
    return findRepositoryByNameAndLandscapeToken(session, repoName, tokenId).orElse(
        new Repository(repoName));
  }
}
