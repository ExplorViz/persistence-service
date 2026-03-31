package net.explorviz.persistence.ogm;

import java.util.SortedSet;
import java.util.TreeSet;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Trace {
  @Id @GeneratedValue private Long id;

  private String traceId;

  private Long startTime;

  private Long endTime;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private final SortedSet<Span> spans = new TreeSet<>();

  public Trace() {
    // Empty constructor required by Neo4j OGM
  }

  public Trace(final String traceId) {
    this.traceId = traceId;
  }

  public void addChildSpan(final Span span) {
    spans.add(span);
    startTime = startTime != null ? Math.min(startTime, span.getStartTime()) : span.getStartTime();
    endTime = endTime != null ? Math.max(endTime, span.getEndTime()) : span.getEndTime();
  }

  public String getTraceId() {
    return traceId;
  }

  public SortedSet<Span> getSpans() {
    return new TreeSet<>(spans);
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(final long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(final long endTime) {
    this.endTime = endTime;
  }
}
