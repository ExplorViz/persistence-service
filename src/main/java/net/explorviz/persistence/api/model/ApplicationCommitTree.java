package net.explorviz.persistence.api.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public class ApplicationCommitTree {
  public String name;
  public List<BranchTree> branches;

  public ApplicationCommitTree() {}

  public ApplicationCommitTree(String name, List<BranchTree> branches) {
    this.name = name;
    this.branches = branches;
  }
}
