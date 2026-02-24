package net.explorviz.persistence.api.model.landscape;

import java.util.List;

public record Application(
    String name,
    String language,
    String instanceId,
    List<Package> packages
) {
  public Application(net.explorviz.persistence.ogm.Application ogmApp) {
    this(ogmApp.getName(), "", "", List.of(new Package(ogmApp.getRootDirectory(), 0)));
  }
}

