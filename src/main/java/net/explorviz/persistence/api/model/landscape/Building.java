package net.explorviz.persistence.api.model.landscape;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record Building(String id, String name, List<String> classIds, List<String> functionIds)
    implements VisualizationObject {
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
