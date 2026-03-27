package net.explorviz.persistence.api.v3.model.landscape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.explorviz.persistence.api.v3.model.MetricValue;
import net.explorviz.persistence.api.v3.model.landscape.FlatBaseModel.FlatConvertible;
import net.explorviz.persistence.api.v3.model.landscape.FunctionDto.FunctionConvertible;

/**
 * Represents a class, specific to the context of analyzing object-oriented programming languages.
 *
 * @param flatBaseModel Container for attributes shared by all flat data objects
 * @param functionIds IDs of all top-level functions which are contained in this class
 */
@RegisterForReflection
public record ClazzDto(
    @JsonUnwrapped FlatBaseModel flatBaseModel,
    List<String> innerClassIds,
    List<String> functionIds,
    @JsonInclude(Include.NON_EMPTY) Map<String, MetricValue> metrics) {

  public ClazzDto {
    Objects.requireNonNull(flatBaseModel);
    Objects.requireNonNull(innerClassIds);
    Objects.requireNonNull(functionIds);
    Objects.requireNonNull(metrics);
  }

  /** Must be implemented by any object which can be represented as a class during flattening. */
  public interface ClassConvertible extends FlatConvertible {
    Collection<? extends ClassConvertible> getInnerClasses();

    Collection<? extends FunctionConvertible> getFunctions();

    Map<String, MetricValue> getMetrics();
  }
}
