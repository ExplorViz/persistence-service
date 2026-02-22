package net.explorviz.persistence.api.model.landscape;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;

@RegisterForReflection
public record Clazz(String id, String name, List<String> functionIds) implements
    VisualizationObject {
  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
