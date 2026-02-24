package net.explorviz.persistence.ogm;

import java.util.stream.Stream;
import net.explorviz.persistence.api.model.flat.VisualizationObject;

public interface Visualizable {
  VisualizationObject toVisualizationObject();

  Stream<Visualizable> getVisualizableChildren();

  default Stream<VisualizationObject> flatten() {
    return Stream.concat(
        Stream.of(toVisualizationObject()),
        getVisualizableChildren().flatMap(Visualizable::flatten));
  }
}
