package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import net.explorviz.persistence.ogm.Application;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class ApplicationRepository {

  private static final String FIND_BY_NAME_AND_LANDSCAPE_TOKEN_STATEMENT = """
      MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(:Trace)-[:CONTAINS]->
            (:Span)-[:BELONGS_TO]->(a:Application {name: $name})
      RETURN a;
      """;

  @Inject
  private SessionFactory sessionFactory;

  public Application findApplicationByNameAndLandscapeToken(final Session session,
                                                            final String name,
                                                            final String tokenId) {
    return session.queryForObject(Application.class, FIND_BY_NAME_AND_LANDSCAPE_TOKEN_STATEMENT,
        Map.of("tokenId", tokenId, "name", name));
  }

  public Application findApplicationByNameAndLandscapeToken(final String name,
                                                            final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findApplicationByNameAndLandscapeToken(session, name, tokenId);
  }
}
