package net.explorviz.persistence.api.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public class BranchTree {
  public String name;
  public List<String> commits;
  public BranchPoint branchPoint;

  public BranchTree() {}

  public BranchTree(String name, List<String> commits, BranchPoint branchPoint) {
    this.name = name;
    this.commits = commits;
    this.branchPoint = branchPoint;
  }
}
