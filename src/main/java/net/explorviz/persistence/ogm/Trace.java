package net.explorviz.persistence.ogm;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
@SuppressWarnings("PMD.SingularField")
public class Trace {
  @Id
  private String traceId;

  private long startTime;

  private long endTime;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Span> spans = new HashSet<>();

  public Trace() {
    // Empty constructor required by Neo4j OGM
  }

  public Trace(final String traceId) {
    this.traceId = traceId;
  }

  public void addChildSpan(final Span span) {
    final Set<Span> newSet = new HashSet<>(spans);
    newSet.add(span);
    spans = Set.copyOf(newSet);
    startTime = Math.min(startTime, span.getStartTime());
    endTime = Math.max(endTime, span.getEndTime());
  }

  public void setStartTime(final long startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(final long endTime) {
    this.endTime = endTime;
  }
}
