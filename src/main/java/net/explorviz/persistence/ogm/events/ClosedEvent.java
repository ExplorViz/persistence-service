package net.explorviz.persistence.ogm.events;

import java.time.Instant;
import net.explorviz.persistence.ogm.Contributor;
import net.explorviz.persistence.ogm.ResourceEvent;
import net.explorviz.persistence.ogm.TrackableResource;

/** Event representing the closure of a TrackableResource. */
public class ClosedEvent extends ResourceEvent {
  public ClosedEvent() {
    // Empty constructor required by Neo4j OGM
    super();
  }

  public ClosedEvent(
      final Instant timestamp,
      final Contributor contributor,
      final String externalId,
      final TrackableResource resource) {
    super(timestamp, contributor, externalId, "closed", resource);
  }
}
