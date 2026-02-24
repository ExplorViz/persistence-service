package net.explorviz.persistence.api.model.landscape;

import java.util.List;

public record K8sDeployment(
    String name,
    List<K8sPod> k8sPods
) {
}
