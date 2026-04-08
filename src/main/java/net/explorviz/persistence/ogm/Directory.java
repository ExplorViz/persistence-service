package net.explorviz.persistence.ogm;

import java.util.SortedSet;
import java.util.TreeSet;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Directory implements Comparable<Directory> {

  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private final SortedSet<Directory> subdirectories = new TreeSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private final SortedSet<FileRevision> fileRevisions = new TreeSet<>();

  public Directory() {
    // Empty constructor required by Neo4j OGM
  }

  public Directory(final String name) {
    this.name = name;
  }

  public void addSubdirectory(final Directory directory) {
    subdirectories.add(directory);
  }

  public void addFileRevision(final FileRevision fileRevision) {
    fileRevisions.add(fileRevision);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public SortedSet<Directory> getSubdirectories() {
    return new TreeSet<>(subdirectories);
  }

  public SortedSet<FileRevision> getFileRevisions() {
    return new TreeSet<>(fileRevisions);
  }

  @Override
  public int compareTo(final Directory other) {
    final int nameComparison = name.compareTo(other.name);

    if (nameComparison != 0) {
      return nameComparison;
    }

    return id != null && other.id != null
        ? id.compareTo(other.id)
        : Integer.compare(System.identityHashCode(this), System.identityHashCode(other));
  }
}
