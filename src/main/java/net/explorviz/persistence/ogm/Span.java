package net.explorviz.persistence.ogm;

import net.explorviz.persistence.proto.SpanData;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Span {
  @Id
  private String spanId;

  private long startTime;

  private long endTime;

  @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
  private Span parentSpan;

  @Relationship(type = "REPRESENTS", direction = Relationship.Direction.OUTGOING)
  private Function function;

  @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
  private Application application;

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

  public void setParentSpan(final Span span) {
    this.parentSpan = span;
  }

  public void setFunction(final Function function) {
    this.function = function;
  }

  public void setApplication(final Application application) {
    this.application = application;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }
}
