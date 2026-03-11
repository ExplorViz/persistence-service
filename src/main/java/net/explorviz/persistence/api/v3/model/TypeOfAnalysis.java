package net.explorviz.persistence.api.v3.model;

public enum TypeOfAnalysis {
  DYNAMIC("dynamic"),
  STATIC("static");

  private final String name;

  TypeOfAnalysis(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
