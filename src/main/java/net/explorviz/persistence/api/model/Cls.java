package net.explorviz.persistence.api.model;

import java.util.ArrayList;
import java.util.List;

public class Cls extends FlatBaseModel {
  public List<String> functionIds = new ArrayList<>();

  public Cls() {}

  public Cls(String id, String name) {
      super(id, name);
  }
}
