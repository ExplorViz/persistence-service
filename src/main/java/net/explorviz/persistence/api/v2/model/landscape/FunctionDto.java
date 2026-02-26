package net.explorviz.persistence.api.v2.model.landscape;

import net.explorviz.persistence.ogm.Function;

public record FunctionDto(
    String name,
    String methodHash
) {
  public FunctionDto(final Function ogmFunc) {
    this(ogmFunc.getName(), ""); // TODO hash?
  }
}
