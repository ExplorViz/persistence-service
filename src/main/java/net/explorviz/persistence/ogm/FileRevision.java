package net.explorviz.persistence.ogm;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class FileRevision {
  @Id
  @GeneratedValue
  private Long id;

  private String hash;

  private String name;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Function> functions = new HashSet<>();

  public FileRevision() {
    // Empty constructor required by Neo4j OGM
  }

  public FileRevision(final String name) {
    this.name = name;
  }

  public FileRevision(final String hash, final String name) {
    this.hash = hash;
    this.name = name;
  }

  public FileRevision(final String name, final Set<Function> functions) {
    this.name = name;
    this.functions = functions;
  }

  public FileRevision(final String hash, final String name, final Set<Function> functions) {
    this.hash = hash;
    this.name = name;
    this.functions = functions;
  }

  public void addFunction(final Function function) {
    final Set<Function> newFunctions = new HashSet<>(functions);
    newFunctions.add(function);
    functions = Set.copyOf(newFunctions);
  }

  public Long getId() {
    return this.id;
  }

  public String getHash() {
    return this.hash;
  }

  public String getName() {
    return this.name;
  }

  public Set<Function> getFunctions() {
    return this.functions;
  }
}
