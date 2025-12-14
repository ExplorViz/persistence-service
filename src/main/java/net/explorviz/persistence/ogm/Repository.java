package net.explorviz.persistence.ogm;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Repository {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Commit> commits = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Branch> branches = new HashSet<>();

  public Repository() {
    // Empty constructor required by Neo4j OGM
  }

  public Repository(final String name) {
    this.name = name;
  }

  public void addCommit(final Commit commit) {
    final Set<Commit> newCommits = new HashSet<>(commits);
    newCommits.add(commit);
    commits = Set.copyOf(newCommits);
  }

  public void addBranch(final Branch branch) {
    final Set<Branch> newBranches = new HashSet<>(branches);
    newBranches.add(branch);
    branches = Set.copyOf(newBranches);
  }

  public static String stripRepoNameFromUpstreamName(final String upstreamName) {
    final String[] partsOfPath = upstreamName.split("/");
    return partsOfPath[partsOfPath.length - 1];
  }
}
