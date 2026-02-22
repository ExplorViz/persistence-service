package net.explorviz.persistence.ogm;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import net.explorviz.persistence.api.model.landscape.District;
import net.explorviz.persistence.api.model.landscape.VisualizationObject;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Directory implements Visualizable {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Directory> subdirectories = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> fileRevisions = new HashSet<>();

  public Directory() {
    // Empty constructor required by Neo4j OGM
  }

  public Directory(final String name) {
    this.name = name;
  }

  public void addSubdirectory(final Directory directory) {
    final Set<Directory> newSubdirectories = new HashSet<>(subdirectories);
    newSubdirectories.add(directory);
    subdirectories = Set.copyOf(newSubdirectories);
  }

  public void addFileRevision(final FileRevision fileRevision) {
    final Set<FileRevision> newFileRevisions = new HashSet<>(fileRevisions);
    newFileRevisions.add(fileRevision);
    fileRevisions = Set.copyOf(newFileRevisions);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Set<Directory> getSubdirectories() {
    return subdirectories;
  }

  public Set<FileRevision> getFileRevisions() {
    return fileRevisions;
  }

  @Override
  public VisualizationObject toVisualizationObject() {
    return new District(id.toString(), name,
        subdirectories.stream().map(d -> d.getId().toString()).toList(),
        fileRevisions.stream().map(f -> f.getId().toString()).toList());
  }

  @Override
  public Stream<Visualizable> getVisualizableChildren() {
    return Stream.concat(subdirectories.stream(), fileRevisions.stream());
  }
}
