package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Application;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class ApplicationRepository {

  private static final String FIND_BY_NAME_AND_LANDSCAPE_TOKEN_STATEMENT = """
      MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(:Trace)-[:CONTAINS]->
            (:Span)-[:BELONGS_TO]->(a:Application {name: $name})
      OPTIONAL MATCH (a)-[h:HAS_ROOT]->(applicationRoot:Directory)
      RETURN a, h, applicationRoot;
      """;

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Application> findApplicationByNameAndLandscapeToken(final Session session,
      final String name, final String tokenId) {
    return Optional.ofNullable(
        session.queryForObject(Application.class, FIND_BY_NAME_AND_LANDSCAPE_TOKEN_STATEMENT,
            Map.of("tokenId", tokenId, "name", name)));
  }

  public Optional<Application> findApplicationByNameAndLandscapeToken(final String name,
      final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findApplicationByNameAndLandscapeToken(session, name, tokenId);
  }

  public Application getOrCreateApplication(final Session session, final String name,
      final String tokenId) {
    return findApplicationByNameAndLandscapeToken(session, name, tokenId).orElse(
        new Application(name));
  }
}
