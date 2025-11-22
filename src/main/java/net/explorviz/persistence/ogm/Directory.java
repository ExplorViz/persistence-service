package net.explorviz.persistence.ogm;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.List;

@NodeEntity
public class Directory {
    String name;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    List<Directory> subdirectories;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    List<FileRevision> files;
}
