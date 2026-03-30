package net.explorviz.persistence.ogm;

import net.explorviz.persistence.avro.SpanData;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Span implements Comparable<Span> {
  @Id @GeneratedValue private Long id;

  private String spanId;

  private long startTime;

  private long endTime;

  @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
  private Span parentSpan;

  @Relationship(type = "REPRESENTS", direction = Relationship.Direction.OUTGOING)
  private Function function;

  public Span() {
    // Empty constructor required by Neo4j OGM
  }

  public Span(final SpanData spanData) {
    this.spanId = spanData.getSpanId();
    this.startTime = spanData.getStartTime();
    this.endTime = spanData.getEndTime();
  }

  public Span(final String spanId) {
    this.spanId = spanId;
  }

  public String getSpanId() {
    return spanId;
  }

  public Span getParentSpan() {
    return parentSpan;
  }

  public void setParentSpan(final Span span) {
    this.parentSpan = span;
  }

  public Function getFunction() {
    return function;
  }

  public void setFunction(final Function function) {
    this.function = function;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setStartTime(final long startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(final long endTime) {
    this.endTime = endTime;
  }

  @Override
  public int compareTo(final Span other) {
    final int startCompare = Long.compare(startTime, other.startTime);
    return startCompare == 0 ? Long.compare(other.endTime, endTime) : startCompare;
  }
}
