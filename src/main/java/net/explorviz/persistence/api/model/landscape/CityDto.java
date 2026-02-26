package net.explorviz.persistence.api.model.landscape;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import net.explorviz.persistence.api.model.TypeOfAnalysis;

@RegisterForReflection
public record CityDto(String id, String name, TypeOfAnalysis originOfData, List<String> districtIds,
                      List<String> buildingIds) implements VisualizationObject {
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
