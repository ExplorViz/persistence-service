package net.explorviz.persistence.ogm;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
public class Trace {
    String id;

    long startTime;

    long endTime;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    List<Span> spans;
}
