package net.explorviz.persistence.ogm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.explorviz.persistence.api.model.landscape.VisualizationObject;
import net.explorviz.persistence.proto.FunctionData;
import net.explorviz.persistence.proto.ParameterData;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Function implements Visualizable {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  private String returnType;

  private boolean isConstructor;

  private final Set<String> annotations;

  private final Set<String> modifiers;

  private final Set<String> outgoingMethodCalls;

  @Properties
  private final Map<String, Double> metrics;

  private int startLine;

  private int endLine;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Parameter> parameters = new HashSet<>();

  public Function() {
    this.annotations = new HashSet<>();
    this.modifiers = new HashSet<>();
    this.outgoingMethodCalls = new HashSet<>();
    this.metrics = new HashMap<>();
  }

  public Function(final String name) {
    this.name = name;
    this.annotations = new HashSet<>();
    this.modifiers = new HashSet<>();
    this.outgoingMethodCalls = new HashSet<>();
    this.metrics = new HashMap<>();
  }

  public Function(final FunctionData functionData) {
    this.name = functionData.getName();
    this.returnType = functionData.getReturnType();
    this.isConstructor = functionData.getIsConstructor();
    this.annotations = new HashSet<>(functionData.getAnnotationsList());
    this.modifiers = new HashSet<>(functionData.getModifiersList());
    this.outgoingMethodCalls = new HashSet<>(functionData.getOutgoingMethodCallsList());
    this.metrics = functionData.getMetricsMap();
    this.startLine = functionData.getStartLine();
    this.endLine = functionData.getEndLine();
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Map<String, Double> getMetrics() {
    return metrics;
  }

  public void addParameter(final Parameter parameter) {
    final Set<Parameter> newParameters = new HashSet<>(parameters);
    newParameters.add(parameter);
    parameters = Set.copyOf(newParameters);
  }

  public void addParameters(final List<ParameterData> parameterDataList) {
    for (final ParameterData p : parameterDataList) {
      addParameter(new Parameter(p.getName(), p.getType(), p.getModifiersList()));
    }
  }

  @Override
  public VisualizationObject toVisualizationObject() {
    return new net.explorviz.persistence.api.model.landscape.Function(id.toString(), name, metrics);
  }

  @Override
  public Stream<Visualizable> getVisualizableChildren() {
    return Stream.empty();
  }
}
