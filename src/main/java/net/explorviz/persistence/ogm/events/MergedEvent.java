package net.explorviz.persistence.ogm.events;

import net.explorviz.persistence.ogm.ResourceEvent;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class MergedEvent extends ResourceEvent {
  private String mergeCommitHash;

  public String getMergeCommitHash() {
    return mergeCommitHash;
  }

  public void setMergeCommitHash(final String mergeCommitHash) {
    this.mergeCommitHash = mergeCommitHash;
  }
}
