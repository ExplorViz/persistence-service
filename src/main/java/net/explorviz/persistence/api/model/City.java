package net.explorviz.persistence.api.model;

import java.util.ArrayList;
import java.util.List;

public class City extends FlatBaseModel {
  public List<String> rootDistrictIds = new ArrayList<>();
  public List<String> districtIds = new ArrayList<>();
  public List<String> buildingIds = new ArrayList<>();

  public City() {}
  
  public City(String id, String name) {
      super(id, name);
  }
}
