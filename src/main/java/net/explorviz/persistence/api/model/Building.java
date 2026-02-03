package net.explorviz.persistence.api.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Building extends FlatBaseModel {
  public String parentCityId;
  public String parentDistrictId;
  public Language language;
  public List<String> classIds = new ArrayList<>();
  public List<String> functionIds = new ArrayList<>();
  public Map<String, Double> metrics = new HashMap<>();

  public Building() {}

  public Building(String id, String name, String parentCityId, String parentDistrictId) {
      super(id, name);
      this.parentCityId = parentCityId;
      this.parentDistrictId = parentDistrictId;
  }
}
