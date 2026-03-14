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

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Repository> findRepositoryByNameAndLandscapeToken(final Session session,
      final String name, final String tokenId) {
    return Optional.ofNullable(session.queryForObject(Repository.class, """
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(r:Repository {name: $name})
        MATCH (r)-[rel]->(n)
        RETURN r, rel, n;
        """, Map.of("tokenId", tokenId, "name", name)));
  }

  public Repository getOrCreateRepository(final Session session, final String repoName,
      final String tokenId) {
    return findRepositoryByNameAndLandscapeToken(session, repoName, tokenId).orElse(
        new Repository(repoName));
  }
}
