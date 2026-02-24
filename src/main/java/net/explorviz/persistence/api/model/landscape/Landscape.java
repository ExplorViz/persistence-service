package net.explorviz.persistence.api.model.landscape;

import java.util.List;

public record Landscape(
    String landscapeToken,
    List<Node> nodes,
    List<K8sNode> k8sNodes
) {
}

