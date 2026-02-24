package net.explorviz.persistence.api.model.flat;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

@RegisterForReflection
public record Function(String id, String name, Map<String, Double> metrics)
    implements VisualizationObject {
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
