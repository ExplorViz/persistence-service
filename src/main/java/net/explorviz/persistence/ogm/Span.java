package net.explorviz.persistence.ogm;

import net.explorviz.persistence.proto.SpanData;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Span {

    @Id
    String spanId;

    long startTime;

    long endTime;

    @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
    Span parentSpan;

    @Relationship(type = "REPRESENTS", direction = Relationship.Direction.OUTGOING)
    Function function;

    public Span() {
    }

    public Span(SpanData spanData) {
        this.spanId = spanData.getId();
        this.startTime = spanData.getStartTime();
        this.endTime = spanData.getEndTime();
    }
}
