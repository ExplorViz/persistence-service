package net.explorviz.persistence.api.model.landscape;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a function / method in source code.
 *
 * @param flatBaseModel Container for attributes shared by all flat data objects
 * @param parentId      ID of the flat data model to which this function belongs
 * @param metrics       Code metrics for this function, i.e. numerical measurements gathered through
 *                      analysis, such as cyclomatic complexity or lines of code
 */
@RegisterForReflection
public record FunctionDto(@JsonUnwrapped FlatBaseModel flatBaseModel, String parentId,
                          Map<String, Double> metrics) {
  public FunctionDto {
    Objects.requireNonNull(flatBaseModel);
    Objects.requireNonNull(parentId);
    Objects.requireNonNull(metrics);
  }
}
