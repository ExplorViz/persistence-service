package net.explorviz.persistence.api.model;

import java.util.ArrayList;
import java.util.List;

public class District extends FlatBaseModel {
  public String parentCityId;
  public String parentDistrictId;
  public List<String> districtIds = new ArrayList<>();
  public List<String> buildingIds = new ArrayList<>();

  public District() {}

  public District(String id, String name, String parentCityId) {
      super(id, name);
      this.parentCityId = parentCityId;
  }
}
