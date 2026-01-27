package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Tag;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class TagRepository {

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Tag> findTagByName(final Session session,
      final String name) {
    return Optional.ofNullable(
        session.queryForObject(Tag.class, """
                MATCH (t:Tag {name: $name})
                RETURN t;
                """,
            Map.of("name", name)));
  }
}
