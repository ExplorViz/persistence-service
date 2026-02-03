package net.explorviz.persistence.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlatBaseModel {
  public String id;
  public String name;
  public String fqn;
  public String originOfData; // TypeOfAnalysis as String
  public String commitComparison; // 'added' | 'modified' | 'removed' | 'unchanged'
  public String editingState; // 'added' | 'removed'

  public FlatBaseModel() {}

  public FlatBaseModel(String id, String name) {
    this.id = id;
    this.name = name;
  }
}
