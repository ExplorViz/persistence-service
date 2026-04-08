package net.explorviz.persistence.ogm;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Application {
  /**
   * From runtime data alone, the name to give to the root directory of the application is not
   * usually known, as paths are usually given relative to the application root. This information
   * may become known at a later point when repository analysis data for the same application
   * arrives. If runtime data arrives first, we use this placeholder name for the root directory.
   */
  public static final String ROOT_NAME_PLACEHOLDER_RUNTIME = "*";

  @Id @GeneratedValue private Long id;

  private String name;

  @Relationship(type = "HAS_ROOT", direction = Relationship.Direction.OUTGOING)
  private Directory rootDirectory;

  public Application() {
    // Empty constructor required by Neo4j OGM
  }

  public Application(final String name) {
    this.name = name;
  }

  public Directory getRootDirectory() {
    return rootDirectory;
  }

  public void setRootDirectory(final Directory rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
