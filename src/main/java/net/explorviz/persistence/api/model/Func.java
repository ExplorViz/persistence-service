package net.explorviz.persistence.api.model;

import java.util.HashMap;
import java.util.Map;

public class Func extends FlatBaseModel {
  public String parentId;
  public Map<String, Double> metrics = new HashMap<>();

  public Func() {}

  public Func(String id, String name, String parentId) {
      super(id, name);
      this.parentId = parentId;
  }
}
