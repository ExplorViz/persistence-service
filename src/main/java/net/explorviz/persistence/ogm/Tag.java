package net.explorviz.persistence.ogm;

import org.neo4j.ogm.annotation.Id;

public class Tag {
  @Id
  private String name;

  public Tag() {
    // Empty constructor required by Neo4j OGM
  }

  public Tag(final String name) {
    this.name = name;
  }

  public String getName() {
    return this.name;
  }
}
