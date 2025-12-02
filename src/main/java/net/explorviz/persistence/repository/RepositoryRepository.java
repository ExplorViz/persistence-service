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

  public Repository getOrCreateRepository(final Session session, final String repoName,
      final String tokenId) {
    return findRepositoryByNameAndLandscapeToken(session, repoName, tokenId).orElse(
        new Repository(repoName));
  }
}
