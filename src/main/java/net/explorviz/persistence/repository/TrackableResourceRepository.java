package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.ResourceEvent;
import net.explorviz.persistence.ogm.TrackableResource;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class TrackableResourceRepository {

  @Inject SessionFactory sessionFactory;

  /**
   * Finds a TrackableResource (Issue or PullRequest) by its number. Assuming it belongs to a
   * specific Repository and Landscape.
   */
  public <T extends TrackableResource> Optional<T> findByNumber(
      final Session session,
      final Class<T> type,
      final Integer number,
      final String repoName,
      final String tokenId) {
    return Optional.ofNullable(
        session.queryForObject(
            type,
            """
            MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:CONTAINS]->(t:%s {number: $number})
            RETURN t;
            """
                .formatted(type.getSimpleName()),
            Map.of("tokenId", tokenId, "repoName", repoName, "number", number)));
  }

  public <T extends TrackableResource> void addEventToResource(
      final Session session,
      final Class<T> resourceType,
      final Integer number,
      final String repoName,
      final String tokenId,
      final ResourceEvent event) {
    final Optional<T> resourceOpt = findByNumber(session, resourceType, number, repoName, tokenId);
    if (resourceOpt.isPresent()) {
      final T resource = resourceOpt.get();

      resource.addEvent(event);
      session.save(resource, 1);
    } else {
      throw new IllegalArgumentException("Resource not found for number: " + number);
    }
  }

  public <T extends TrackableResource> T getOrCreate(
      final Session session,
      final Class<T> type,
      final Integer number,
      final String repoName,
      final String tokenId) {
    return findByNumber(session, type, number, repoName, tokenId)
        .orElseGet(
            () -> {
              try {
                final T resource = type.getDeclaredConstructor().newInstance();
                resource.setNumber(number);
                return resource;
              } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(
                    "Could not instantiate TrackableResource of type: " + type.getSimpleName(), e);
              }
            });
  }
}
