package net.explorviz.persistence.ogm.events;

import java.time.Instant;
import net.explorviz.persistence.ogm.Contributor;
import net.explorviz.persistence.ogm.ResourceEvent;
import net.explorviz.persistence.ogm.TrackableResource;

/** Event representing the creation (e.g., opening) of a TrackableResource. */
public class CreatedEvent extends ResourceEvent {
  public CreatedEvent() {
    // Empty constructor required by Neo4j OGM
    super();
  }

  public CreatedEvent(
      final Instant timestamp,
      final Contributor contributor,
      final String externalId,
      final TrackableResource resource) {
    super(timestamp, contributor, externalId, "created", resource);
  }
}
