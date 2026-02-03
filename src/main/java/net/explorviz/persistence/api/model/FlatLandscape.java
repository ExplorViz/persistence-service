package net.explorviz.persistence.api.model;

import java.util.HashMap;
import java.util.Map;

public class FlatLandscape {
  public String landscapeToken;
  public Map<String, City> cities = new HashMap<>();
  public Map<String, District> districts = new HashMap<>();
  public Map<String, Building> buildings = new HashMap<>();
  public Map<String, Cls> classes = new HashMap<>();
  public Map<String, Func> functions = new HashMap<>();

  public FlatLandscape() {}

  public FlatLandscape(String landscapeToken) {
    this.landscapeToken = landscapeToken;
  }
}
