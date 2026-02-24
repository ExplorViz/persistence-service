package net.explorviz.persistence.api.model.landscape;

import java.util.List;

public record K8sPod(
    String name,
    List<Application> applications
) {
}
