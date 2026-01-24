package net.explorviz.persistence.ogm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.explorviz.persistence.proto.FunctionData;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class Function {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  private String returnType;

  private boolean isConstructor;

  private Set<String> annotations = new HashSet<>();

  private Set<String> modifiers = new HashSet<>();

  // TODO: Decide how to handle parameters (own node class?)

  private Set<String> outgoingMethodCalls = new HashSet<>();

  private Map<String, Double> metrics = new HashMap<>();

  private int startLine;

  private int endLine;

  public Function() {
    // Empty constructor required by Neo4j OGM
  }

  public Function(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
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
}
