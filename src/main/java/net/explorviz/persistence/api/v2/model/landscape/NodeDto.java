package net.explorviz.persistence.api.v2.model.landscape;

import java.util.List;

public record NodeDto(
    String ipAddress,
    String hostName,
    List<ApplicationDto> applications
) {
}
