package net.explorviz.persistence.ogm;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.Relationship.Direction;

@NodeEntity
@SuppressWarnings("PMD.SingularField")
public class Commit {
  @Id
  private String hash;

  private String author;

  private ZonedDateTime date;

  @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
  private Branch branch;

  @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
  private Set<Commit> parentCommits = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> fileRevisions = new HashSet<>();

  public Commit() {
    // Empty constructor required by Neo4j OGM
  }

  public Commit(final String hash) {
    this.hash = hash;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public void addParent(final Commit commit) {
    final Set<Commit> newParentCommits = new HashSet<>(parentCommits);
    newParentCommits.add(commit);
    parentCommits = Set.copyOf(newParentCommits);
  }

  public void addFileRevision(final FileRevision fileRevision) {
    final Set<FileRevision> newFileRevisions = new HashSet<>(fileRevisions);
    newFileRevisions.add(fileRevision);
    fileRevisions = Set.copyOf(newFileRevisions);
  }
}
