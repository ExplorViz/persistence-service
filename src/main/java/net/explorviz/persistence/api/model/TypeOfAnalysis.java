package net.explorviz.persistence.api.model;

public enum TypeOfAnalysis {
  DYNAMIC("dynamic"),
  STATIC("static");

  private final String name;

  TypeOfAnalysis(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
