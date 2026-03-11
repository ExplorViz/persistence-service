package net.explorviz.persistence.api.v3.model;

public enum EditingState {
  ADDED("added"),
  REMOVED("removed");

  private final String name;

  EditingState(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
