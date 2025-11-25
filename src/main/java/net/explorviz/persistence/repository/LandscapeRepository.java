package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import net.explorviz.persistence.ogm.Landscape;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class LandscapeRepository {

  @Inject
  private SessionFactory sessionFactory;

  public Landscape findLandscapeByTokenId(final Session session, final String tokenId) {
    return session.queryForObject(Landscape.class,
        "MATCH (l:Landscape {tokenId: $tokenId}) RETURN l;", Map.of("tokenId", tokenId));
  }

  public Landscape findLandscapeByTokenId(final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findLandscapeByTokenId(session, tokenId);
  }
}
