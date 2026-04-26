package net.explorviz.persistence.ogm;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.DateLong;

@NodeEntity
@RegisterForReflection
public class PullRequest {
  @Id @GeneratedValue private Long id;

  private Integer number;
  private String title;
  private String state;
  private Set<String> labels;
  @DateLong private Instant createdAt;
  @DateLong private Instant closedAt;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private final Set<Commit> commits = new HashSet<>();

  @Relationship(type = "REFERENCES", direction = Relationship.Direction.OUTGOING)
  private Issue issue;

  public PullRequest() {
    // Empty constructor required by Neo4j OGM
  }

  public PullRequest(
      final Integer number,
      final String title,
      final String state,
      final Set<String> labels,
      final Instant createdAt,
      final Instant closedAt) {
    this.number = number;
    this.title = title;
    this.state = state;
    this.labels = labels != null ? new HashSet<>(labels) : new HashSet<>();
    this.createdAt = createdAt;
    this.closedAt = closedAt;
  }

  public Long getId() {
    return id;
  }

  public Integer getNumber() {
    return number;
  }

  public void setNumber(final Integer number) {
    this.number = number;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(final String title) {
    this.title = title;
  }

  public String getState() {
    return state;
  }

  public void setState(final String state) {
    this.state = state;
  }

  public Set<String> getLabels() {
    return labels;
  }

  public void setLabels(final Set<String> labels) {
    this.labels = labels != null ? new HashSet<>(labels) : new HashSet<>();
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(final Instant closedAt) {
    this.closedAt = closedAt;
  }
}
