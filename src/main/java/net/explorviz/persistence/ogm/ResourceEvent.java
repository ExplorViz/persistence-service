package net.explorviz.persistence.ogm;

import java.time.Instant;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class ResourceEvent {
  @Id @GeneratedValue private Long id;

  private String externalId; // e.g., GitHub event ID

  private Instant timestamp;

  private String eventType; // e.g., "opened", "closed", "labeled", etc. (ENUM?)

  @Relationship(type = "PERFORMED_BY", direction = Relationship.Direction.OUTGOING)
  private Contributor contributor;

  @Relationship(type = "NEXT_EVENT", direction = Relationship.Direction.OUTGOING)
  private ResourceEvent nextEvent;

  @Relationship(type = "HAS_EVENT", direction = Relationship.Direction.INCOMING)
  private TrackableResource resource;

  public ResourceEvent() {
    // Empty constructor required by Neo4j OGM
  }

  public ResourceEvent(
      final Instant timestamp,
      final Contributor contributor,
      final String externalId,
      final String eventType,
      final TrackableResource resource) {
    this.timestamp = timestamp;
    this.contributor = contributor;
    this.externalId = externalId;
    this.eventType = eventType;
    this.resource = resource;
  }

  public Long getId() {
    return id;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(final String externalId) {
    this.externalId = externalId;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Instant timestamp) {
    this.timestamp = timestamp;
  }

  public Contributor getContributor() {
    return contributor;
  }

  public void setContributor(final Contributor contributor) {
    this.contributor = contributor;
  }

  public ResourceEvent getNextEvent() {
    return nextEvent;
  }

  public void setNextEvent(final ResourceEvent nextEvent) {
    this.nextEvent = nextEvent;
  }

  public TrackableResource getResource() {
    return resource;
  }

  public void setResource(final TrackableResource resource) {
    this.resource = resource;
  }
}
