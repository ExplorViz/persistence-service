package net.explorviz.persistence.ogm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.explorviz.persistence.api.model.TypeOfAnalysis;
import net.explorviz.persistence.api.model.landscape.City;
import net.explorviz.persistence.api.model.landscape.VisualizationObject;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Repository implements Visualizable {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Commit> commits = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Branch> branches = new HashSet<>();

  @Relationship(type = "HAS_ROOT", direction = Relationship.Direction.OUTGOING)
  private Directory rootDirectory;

  public Repository() {
    // Empty constructor required by Neo4j OGM
  }

  public Repository(final String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
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

  public void addRootDirectory(final Directory directory) {
    this.rootDirectory = directory;
  }

  public static String stripRepoNameFromUpstreamName(final String upstreamName) {
    final String[] partsOfPath = upstreamName.split("/");
    return partsOfPath[partsOfPath.length - 1];
  }

  public Directory getRootDirectory() {
    return this.rootDirectory;
  }


  @Override
  public VisualizationObject toVisualizationObject() {
    return new City(id.toString(), getName(), TypeOfAnalysis.STATIC,
        List.of(rootDirectory.getId().toString()), List.of());
  }

  @Override
  public Stream<Visualizable> getVisualizableChildren() {
    return Stream.of(rootDirectory);
  }
}
