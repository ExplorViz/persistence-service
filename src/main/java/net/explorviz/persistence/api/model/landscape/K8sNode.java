package net.explorviz.persistence.api.model.landscape;

import java.util.List;

public record K8sNode(
    String name,
    List<K8sNamespace> k8sNamespaces
) {
}
