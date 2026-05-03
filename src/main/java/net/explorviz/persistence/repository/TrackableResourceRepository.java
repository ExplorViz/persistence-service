package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.AnnotationType;
import net.explorviz.persistence.ogm.Contributor;
import net.explorviz.persistence.ogm.ResourceAnnotation;
import net.explorviz.persistence.ogm.ResourceVersion;
import net.explorviz.persistence.ogm.TrackableResource;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@SuppressWarnings("PMD.ExcessiveParameterList")
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

  /**
   * * Adds an annotation to a resource, creating both a ResourceAnnotation activity and a new *
   * ResourceVersion. According to PROV semantics: - The annotation USED the previous *
   * ResourceVersion (if it exists) - The annotation GENERATED a new ResourceVersion - The new *
   * version is linked to the resource via HAS_VERSION * * @param session The Neo4j OGM session
   * * @param resourceType The type of resource (Issue or PullRequest) * @param number The resource
   * number * @param repoName The repository name * @param tokenId The landscape token ID * @param
   * externalId The external ID of the event (e.g., GitHub event ID) * @param timestamp The time of
   * the annotation * @param annotationType The type of annotation (e.g., "created", "closed",
   * "labeled") * @param contributor The user who performed the annotation * @param newVersion The
   * new version of the resource (snapshot after annotation) * @return The ResourceAnnotation that
   * was created
   */
  public <T extends TrackableResource> ResourceAnnotation addAnnotationAndVersion(
      final Session session,
      final Class<T> resourceType,
      final Integer number,
      final String repoName,
      final String tokenId,
      final String externalId,
      final Instant timestamp,
      final AnnotationType annotationType,
      final Contributor contributor,
      final ResourceVersion newVersion) {
    final Optional<T> resourceOpt = findByNumber(session, resourceType, number, repoName, tokenId);
    if (resourceOpt.isEmpty()) {
      throw new IllegalArgumentException("Resource not found for number: " + number);
    }

    final T resource = resourceOpt.get();

    // Get the current (previous) version if it exists
    final ResourceVersion previousVersion = resource.getCurrentVersion();

    // Create the annotation activity
    final ResourceAnnotation annotation = new ResourceAnnotation();
    annotation.setExternalId(externalId);
    annotation.setTimestamp(timestamp);
    annotation.setAnnotationType(annotationType);
    annotation.setContributor(contributor);

    // Link the annotation to the versions
    annotation.setUsedResource(
        previousVersion); // Used the previous version (can be null for "created"))
    annotation.setGeneratedResource(newVersion); // Generated the new version

    // Link the new version back to the annotation and resource
    newVersion.setGeneratedBy(annotation);
    newVersion.setResource(resource);
    newVersion.setCreatedBy(contributor);
    newVersion.setCreationDate(timestamp);
    if (previousVersion != null) {
      newVersion.setDerivedFrom(previousVersion);
    }

    // Add the version to the resource
    resource.addVersion(newVersion);

    // Save all entities in transaction
    session.save(annotation, 2); // depth 2 to cascade to resourceVersions and contributor
    session.save(resource, 1); // depth 1 sufficient since annotation is already saved

    return annotation;
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
