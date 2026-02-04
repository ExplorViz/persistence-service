package net.explorviz.persistence.ogm;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Directory {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Directory> subdirectories = new HashSet<>();

  public Directory() {
    // Empty constructor required by Neo4j OGM
  }

  public Directory(final String name) {
    this.name = name;
  }

  public void addSubdirectory(final Directory directory) {
    subdirectories.add(directory);
    directory.setParent(this);
  }

  public Set<Directory> getSubdirectories() {
    return subdirectories;
  }

  public String getName() {
    return name;
  }
  public Long getId() {
    return id;
  }

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.INCOMING)
  private Directory parent;

  public Directory getParent() {
    return parent;
  }

  public void setParent(final Directory parent) {
    this.parent = parent;
  }
}
