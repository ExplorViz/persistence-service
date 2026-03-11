package net.explorviz.persistence.api.v3.model;

public enum CommitComparison {
  ADDED("added"),
  MODIFIED("modified"),
  REMOVED("removed"),
  UNCHANGED("unchanged");

  private final String name;

  CommitComparison(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
