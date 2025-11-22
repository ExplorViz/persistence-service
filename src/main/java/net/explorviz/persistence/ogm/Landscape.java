package net.explorviz.persistence.ogm;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
public class Landscape {
    String token_id;

    String token_secret;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    List<Trace> traces;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    List<Repository> repositories;
}
