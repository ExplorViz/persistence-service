package net.explorviz.persistence.ogm;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Landscape {
  @Id
  private String tokenId;

  private long createdAt;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Trace> traces = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Repository> repositories = new HashSet<>();

  public Landscape() {
    // Empty constructor required by Neo4j OGM
  }

  public Landscape(final String tokenId, final Set<Trace> traces) {
    this.tokenId = tokenId;
    this.traces = traces;
    this.createdAt = Instant.now().toEpochMilli();
  }

  public Landscape(final String tokenId) {
    this.tokenId = tokenId;
    this.createdAt = Instant.now().toEpochMilli();
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

  public String getTokenId() {
    return this.tokenId;
  }

  public long getCreatedAt() {
    return this.createdAt;
  }
}
