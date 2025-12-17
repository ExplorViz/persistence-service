package net.explorviz.persistence.ogm;

import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Landscape {
  @Id
  private String tokenId;

  private String tokenSecret;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Trace> traces = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Repository> repositories = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Application> applications = new HashSet<>();

  public Landscape() {
    // Empty constructor required by Neo4j OGM
  }

  public Landscape(final String tokenId, final String tokenSecret, final Set<Trace> traces) {
    this.tokenId = tokenId;
    this.tokenSecret = tokenSecret;
    this.traces = traces;
  }

  public Landscape(final String tokenId) {
    this.tokenId = tokenId;
  }

  public void addTrace(final Trace trace) {
    final Set<Trace> newTraces = new HashSet<>(traces);
    newTraces.add(trace);
    traces = Set.copyOf(newTraces);
  }

  public void addRepository(final Repository repo) {
    final Set<Repository> newRepositories = new HashSet<>(repositories);
    newRepositories.add(repo);
    repositories = Set.copyOf(newRepositories);
  }

  public void addApplication(final Application application) {
    final Set<Application> newApplications = new HashSet<>(applications);
    newApplications.add(application);
    applications = Set.copyOf(newApplications);
  }
}
