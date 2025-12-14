package net.explorviz.persistence.ogm;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class Function {
  @Id
  @GeneratedValue
  private Long id;

  private String fqn;

  public Function() {
    // Empty constructor required by Neo4j OGM
  }

  public Function(final String fqn) {
    this.fqn = fqn;
  }

  public String getFqn() {
    return fqn;
  }
}
