package net.explorviz.persistence.api.model.landscape;

import net.explorviz.persistence.ogm.Function;

public record Method(
    String name,
    String methodHash
) {
  public Method(final Function ogmFunc) {
    this(ogmFunc.getName(), ""); // TODO hash?
  }
}
