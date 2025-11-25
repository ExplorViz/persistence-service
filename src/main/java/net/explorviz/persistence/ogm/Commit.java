package net.explorviz.persistence.ogm;

import java.time.ZonedDateTime;
import java.util.Set;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
@SuppressWarnings("PMD.SingularField")
public class Commit {
  private String hash;

  private String author;

  private ZonedDateTime date;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> files;
}
