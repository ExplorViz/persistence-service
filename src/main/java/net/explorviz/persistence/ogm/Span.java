package net.explorviz.persistence.ogm;

import net.explorviz.persistence.proto.SpanData;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
@SuppressWarnings("PMD.SingularField")
public class Span {

  @Id
  public String spanId;

  public long startTime;

  public long endTime;

  @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
  private Span parentSpan;

  @Relationship(type = "REPRESENTS", direction = Relationship.Direction.OUTGOING)
  private Function function;

  public Span() {
    // Empty constructor required by Neo4j OGM
  }

  public Span(final SpanData spanData) {
    this.spanId = spanData.getId();
    this.startTime = spanData.getStartTime();
    this.endTime = spanData.getEndTime();
  }
}
