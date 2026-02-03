package net.explorviz.persistence.api.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class BranchPoint {
  public String name;
  public String commit;

  public BranchPoint() {}

  public BranchPoint(String name, String commit) {
    this.name = name;
    this.commit = commit;
  }
}
