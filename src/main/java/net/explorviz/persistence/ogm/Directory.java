package net.explorviz.persistence.ogm;

import java.util.List;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Directory {
  private String name;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private List<Directory> subdirectories;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private List<FileRevision> files;
}
