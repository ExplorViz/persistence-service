package net.explorviz.persistence.api.model.landscape;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a class, specific to the context of analyzing object-oriented programming languages.
 *
 * @param flatBaseModel Container for attributes shared by all flat data objects
 * @param functionIds   IDs of all top-level functions which are contained in this class
 */
@RegisterForReflection
public record ClazzDto(@JsonUnwrapped FlatBaseModel flatBaseModel, List<String> functionIds) {
  public ClazzDto {
    Objects.requireNonNull(flatBaseModel);
    Objects.requireNonNull(functionIds);
  }
}
