package net.explorviz.persistence.ogm;

import java.util.List;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Landscape {
  private String tokenId;

  private String tokenSecret;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private List<Trace> traces;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private List<Repository> repositories;
}
