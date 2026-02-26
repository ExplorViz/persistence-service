package net.explorviz.persistence.api.v2.model.landscape;

import java.util.List;

public record ApplicationDto(
    String name,
    String language,
    String instanceId,
    List<PackageDto> packages
) {
  public ApplicationDto(final net.explorviz.persistence.ogm.Application ogmApp) {
    this(ogmApp.getName(), "", "", List.of(new PackageDto(ogmApp.getRootDirectory(), 0)));
  }
}

