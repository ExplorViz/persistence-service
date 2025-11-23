package net.explorviz.persistence.ogm;

import java.util.List;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class FileRevision {
  private String fqn;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private List<Function> functions;
}
