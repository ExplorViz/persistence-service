package net.explorviz.persistence.ogm;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.DateLong;

@NodeEntity
@RegisterForReflection
public class Commit {
  @Id @GeneratedValue private Long id;

  private String hash;

  @DateLong private Instant authorDate;

  @DateLong private Instant commitDate;

  @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
  private Branch branch;

  @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
  private Set<Commit> parentCommits = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> fileRevisions = new HashSet<>();

  @Relationship(type = "ADDED", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> addedFileRevisions = new HashSet<>();

  @Relationship(type = "DELETED", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> deletedFileRevisions = new HashSet<>();

  @Relationship(type = "MODIFIED", direction = Relationship.Direction.OUTGOING)
  private Set<FileRevision> modifiedFileRevisions = new HashSet<>();

  @Relationship(type = "IS_TAGGED_WITH", direction = Relationship.Direction.OUTGOING)
  private Set<Tag> tags = new HashSet<>();

  @Relationship(type = "AUTHORED", direction = Relationship.Direction.INCOMING)
  private Contributor author;

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

  public void addAddedFileRevision(final FileRevision fileRevision) {
    final Set<FileRevision> newAddedFileRevisions = new HashSet<>(addedFileRevisions);
    newAddedFileRevisions.add(fileRevision);
    addedFileRevisions = Set.copyOf(newAddedFileRevisions);
  }

  public void addDeletedFileRevision(final FileRevision fileRevision) {
    final Set<FileRevision> newDeletedFileRevisions = new HashSet<>(deletedFileRevisions);
    newDeletedFileRevisions.add(fileRevision);
    deletedFileRevisions = Set.copyOf(newDeletedFileRevisions);
  }

  public void addModifiedFileRevision(final FileRevision fileRevision) {
    final Set<FileRevision> newModifiedFileRevisions = new HashSet<>(modifiedFileRevisions);
    newModifiedFileRevisions.add(fileRevision);
    modifiedFileRevisions = Set.copyOf(newModifiedFileRevisions);
  }

  public void addTag(final Tag tag) {
    final Set<Tag> newTags = new HashSet<>(tags);
    newTags.add(tag);
    tags = Set.copyOf(newTags);
  }

  public Branch getBranch() {
    return branch;
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

  public void setAuthorDate(final Instant authorDate) {
    this.authorDate = authorDate;
  }

  public Set<Commit> getParentCommits() {
    return parentCommits;
  }

  public Set<FileRevision> getFileRevisions() {
    return fileRevisions;
  }

  public Contributor getAuthor() {
    return author;
  }

  public void setAuthor(final Contributor author) {
    this.author = author;
  }
}
