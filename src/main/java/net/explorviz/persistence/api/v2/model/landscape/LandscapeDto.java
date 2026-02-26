package net.explorviz.persistence.api.v2.model.landscape;

import java.util.List;

public record LandscapeDto(
    String landscapeToken,
    List<NodeDto> nodes,
    List<K8sNodeDto> k8sNodes
) {
}

