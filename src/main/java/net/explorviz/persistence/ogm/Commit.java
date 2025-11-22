package net.explorviz.persistence.ogm;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.time.ZonedDateTime;
import java.util.List;

@NodeEntity
public class Commit {
    String hash;

    String author;

    ZonedDateTime date;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    List<FileRevision> files;
}
