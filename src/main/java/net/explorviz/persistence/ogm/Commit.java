package net.explorviz.persistence.ogm;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
@RegisterForReflection
public class Commit {
  @Id
  private String hash;

  private String author;

  private Instant authorDate;

  private Instant commitDate;

  @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
  private Branch branch;

  @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
  private Set<Commit> parentCommits = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> fileRevisions = new HashSet<>();

  @Relationship(type = "IS_TAGGED_WITH", direction = Relationship.Direction.OUTGOING)
  private Set<Tag> tags = new HashSet<>();

  public Commit() {
    // Empty constructor required by Neo4j OGM
  }

  public Commit(final String hash) {
    this.hash = hash;
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

  public void addTag(final Tag tag) {
    final Set<Tag> newTags = new HashSet<>(tags);
    newTags.add(tag);
    tags = Set.copyOf(newTags);
  }

  public void setBranch(final Branch branch) {
    this.branch = branch;
  }

  public String getHash() {
    return this.hash;
  }

  public Instant getCommitDate() {
    return commitDate;
  }

  public void setCommitDate(final Instant commitDate) {
    this.commitDate = commitDate;
  }
  public Set<FileRevision> getFileRevisions() {
    return fileRevisions;
  }
}
