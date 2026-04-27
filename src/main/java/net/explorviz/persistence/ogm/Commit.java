package net.explorviz.persistence.ogm;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.DateLong;

/** Represents a git commit. */
@NodeEntity
public class Commit {

  /**
   * Use a generated ID as opposed to the commit hash since the same commit could theoretically
   * appear in multiple landscapes.
   */
  @Id @GeneratedValue private Long id;

  private String hash;

  private String author;

  @DateLong private Instant authorDate;

  @DateLong private Instant commitDate;

  @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
  private Branch branch;

  @Relationship(type = "HAS_PARENT", direction = Relationship.Direction.OUTGOING)
  private final Set<Commit> parentCommits = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private final Set<FileRevision> fileRevisions = new HashSet<>();

  @Relationship(type = "IS_TAGGED_WITH", direction = Relationship.Direction.OUTGOING)
  private final Set<Tag> tags = new HashSet<>();

  public Commit() {
    // Empty constructor required by Neo4j OGM
  }

  public Commit(final String hash) {
    this.hash = hash;
  }

  public String getHash() {
    return hash;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(final Branch branch) {
    this.branch = branch;
  }

  public Set<Commit> getParentCommits() {
    return Set.copyOf(parentCommits);
  }

  public void addParentCommit(final Commit parentCommit) {
    parentCommits.add(parentCommit);
  }

  public Set<FileRevision> getFileRevisions() {
    return Set.copyOf(fileRevisions);
  }

  public void addFileRevision(final FileRevision fileRevision) {
    fileRevisions.add(fileRevision);
  }

  public Instant getCommitDate() {
    return commitDate;
  }

  public void setCommitDate(final Instant commitDate) {
    this.commitDate = commitDate;
  }

  public Instant getAuthorDate() {
    return authorDate;
  }

  public void setAuthorDate(final Instant authorDate) {
    this.authorDate = authorDate;
  }

  public Set<Tag> getTags() {
    return Set.copyOf(tags);
  }

  public void addTag(final Tag tag) {
    tags.add(tag);
  }
}
