package net.explorviz.persistence.ogm;

import java.util.Set;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
@SuppressWarnings("PMD.SingularField")
public class Directory {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Directory> subdirectories;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> files;
}
