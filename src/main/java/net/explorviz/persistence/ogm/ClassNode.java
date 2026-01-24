package net.explorviz.persistence.ogm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.explorviz.persistence.proto.ClassData;
import net.explorviz.persistence.proto.ClassType;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class ClassNode {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  private ClassType type;

  private Set<String> modifiers = new HashSet<>();

  private Set<String> implementedInterfaces = new HashSet<>();

  // TODO: Might change to a relationship to Classes
  private Set<String> superClasses = new HashSet<>();

  private Set<String> annotations = new HashSet<>();

  // TODO: Decide how to handle fields (own node class?)

  private Set<String> enumValues = new HashSet<>();

  private Map<String, Double> metrics = new HashMap<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<ClassNode> innerClasses = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Function> functions = new HashSet<>();

  public ClassNode() {
    // Empty constructor required by Neo4j OGM
  }

  public ClassNode(final String name) {
    this.name = name;
  }

  public ClassNode(final String name, final ClassType type) {
    this.name = name;
    this.type = type;
  }

  public ClassNode(final ClassData classData) {
    this.name = classData.getName();
    this.type = classData.getType();
    this.modifiers = new HashSet<>(classData.getModifiersList());
    this.implementedInterfaces = new HashSet<>(classData.getImplementedInterfacesList());
    this.superClasses = new HashSet<>(classData.getSuperclassesList());
    this.annotations = new HashSet<>(classData.getAnnotationsList());
    // TODO: Handle fields
    this.enumValues = new HashSet<>(classData.getEnumValuesList());
    this.metrics = classData.getMetricsMap();
  }

  public void addInnerClass(final ClassNode innerClass) {
    final Set<ClassNode> newInnerClasses = new HashSet<>(innerClasses);
    newInnerClasses.add(innerClass);
    innerClasses = Set.copyOf(newInnerClasses);
  }

  public void addFunctions(final Function function) {
    final Set<Function> newFunctions = new HashSet<>(functions);
    newFunctions.add(function);
    functions = Set.copyOf(newFunctions);
  }
}
