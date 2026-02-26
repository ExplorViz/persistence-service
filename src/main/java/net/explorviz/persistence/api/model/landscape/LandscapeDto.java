package net.explorviz.persistence.api.model.landscape;

import java.util.Map;

public record LandscapeDto(String landscapeToken,
                           Map<String, VisualizationObject> visualizationObjects) {
}
