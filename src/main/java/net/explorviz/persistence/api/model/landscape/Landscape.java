package net.explorviz.persistence.api.model.landscape;

import java.util.Map;

public record Landscape(String landscapeToken,
                        Map<String, VisualizationObject> visualizationObjects) {
}
