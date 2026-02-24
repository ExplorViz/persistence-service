package net.explorviz.persistence.api.model.landscape;

import java.util.List;

public record K8sNamespace(
    String name,
    List<K8sDeployment> k8sDeployments
) {
}
