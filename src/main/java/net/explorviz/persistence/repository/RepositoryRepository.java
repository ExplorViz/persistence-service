package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Repository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;


@ApplicationScoped
public class RepositoryRepository {

  private static final String FIND_BY_NAME_AND_LANDSCAPE_TOKEN_STATEMENT = """
      MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(r:Repository {name: $name})
      RETURN r;
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

  public List<Repository> findRepositoriesByLandscapeToken(final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findRepositoriesByLandscapeToken(session, tokenId);
  }

  public List<Repository> findRepositoriesByLandscapeToken(final Session session,
      final String tokenId) {
    return (List<Repository>) session.query(Repository.class, """
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(r:Repository)
        RETURN r;
        """, Map.of("tokenId", tokenId));
  }

  public Repository getOrCreateRepository(final Session session, final String repoName,
      final String tokenId) {
    return findRepositoryByNameAndLandscapeToken(session, repoName, tokenId).orElse(
        new Repository(repoName));
  }

  public Iterable<Map<String, Object>> findCommitTreeData(final Session session,
      final String repoName, final String tokenId) {
    return session.query("""
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(r:Repository {name: $repoName})
        MATCH (r)-[:CONTAINS]->(c:Commit)
        OPTIONAL MATCH (c)-[:BELONGS_TO]->(b:Branch)
        WITH b, c
        ORDER BY coalesce(c.commitDate, 0) ASC
        RETURN b.name as branchName, collect(c.hash) as commitHashes
        """, Map.of("tokenId", tokenId, "repoName", repoName));
  }

  public Iterable<Map<String, Object>> findCommitTreeData(final String repoName,
      final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findCommitTreeData(session, repoName, tokenId);
  }
}
