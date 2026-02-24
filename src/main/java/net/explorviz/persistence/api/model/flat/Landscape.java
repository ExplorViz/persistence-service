package net.explorviz.persistence.api.model.flat;

import java.util.Map;

public record Landscape(String landscapeToken,
                        Map<String, VisualizationObject> visualizationObjects) {
}
