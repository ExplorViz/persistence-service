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
import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Clazz {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  private ClassType type;

  private final Set<String> modifiers;

  private final Set<String> implementedInterfaces;

  // TODO: Might change to a relationship to Classes
  private final Set<String> superClasses;

  private final Set<String> annotations;

  private final Set<String> enumValues;

  @Properties
  private final Map<String, Double> metrics;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Clazz> innerClasses = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Function> functions = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Field> fields = new HashSet<>();

  public Clazz() {
    this.modifiers = new HashSet<>();
    this.implementedInterfaces = new HashSet<>();
    this.superClasses = new HashSet<>();
    this.annotations = new HashSet<>();
    this.enumValues = new HashSet<>();
    this.metrics = new HashMap<>();
  }

  public Clazz(final String name) {
    this.name = name;
    this.modifiers = new HashSet<>();
    this.implementedInterfaces = new HashSet<>();
    this.superClasses = new HashSet<>();
    this.annotations = new HashSet<>();
    this.enumValues = new HashSet<>();
    this.metrics = new HashMap<>();
  }

  public Clazz(final String name, final ClassType type) {
    this.name = name;
    this.type = type;
    this.modifiers = new HashSet<>();
    this.implementedInterfaces = new HashSet<>();
    this.superClasses = new HashSet<>();
    this.annotations = new HashSet<>();
    this.enumValues = new HashSet<>();
    this.metrics = new HashMap<>();
  }

  public Clazz(final ClassData classData) {
    this.name = classData.getName();
    this.type = classData.getType();
    this.modifiers = new HashSet<>(classData.getModifiersList());
    this.implementedInterfaces = new HashSet<>(classData.getImplementedInterfacesList());
    this.superClasses = new HashSet<>(classData.getSuperclassesList());
    this.annotations = new HashSet<>(classData.getAnnotationsList());
    this.enumValues = new HashSet<>(classData.getEnumValuesList());
    this.metrics = classData.getMetricsMap();
  }

  public void addInnerClass(final Clazz innerClass) {
    final Set<Clazz> newInnerClasses = new HashSet<>(innerClasses);
    newInnerClasses.add(innerClass);
    innerClasses = Set.copyOf(newInnerClasses);
  }

  public void addFunction(final Function function) {
    final Set<Function> newFunctions = new HashSet<>(functions);
    newFunctions.add(function);
    functions = Set.copyOf(newFunctions);
  }

  public void addField(final Field field) {
    final Set<Field> newFields = new HashSet<>(fields);
    newFields.add(field);
    fields = Set.copyOf(newFields);
  }
  public Set<Function> getFunctions() {
    return functions;
  }
  public String getName() {
    return name;
  }
}
