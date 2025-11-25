package net.explorviz.persistence.ogm;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
@SuppressWarnings("PMD.SingularField")
public class Application {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  public Application() {
    // Empty constructor required by Neo4j OGM
  }

  public Application(final String name) {
    this.name = name;
  }
}
