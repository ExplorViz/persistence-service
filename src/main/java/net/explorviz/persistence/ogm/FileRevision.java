package net.explorviz.persistence.ogm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.explorviz.persistence.api.model.landscape.Building;
import net.explorviz.persistence.api.model.landscape.VisualizationObject;
import net.explorviz.persistence.proto.Language;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class FileRevision implements Visualizable {
  @Id
  @GeneratedValue
  private Long id;

  private String hash;

  private String name;

  private boolean hasFileData;

  private Language language;

  private String packageName;

  private Set<String> importNames = new HashSet<>();

  @Properties
  private Map<String, Double> metrics = new HashMap<>();

  private String lastEditor;

  private int addedLines;

  private int modifiedLines;

  private int deletedLines;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Clazz> classes = new HashSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private Set<Function> functions = new HashSet<>();

  public FileRevision() {
    // Empty constructor required by Neo4j OGM
  }

  public FileRevision(final String name) {
    this.name = name;
  }

  public FileRevision(final String hash, final String name) {
    this.hash = hash;
    this.name = name;
  }

  public FileRevision(final String name, final Set<Function> functions) {
    this.name = name;
    this.functions = functions;
  }

  public FileRevision(final String hash, final String name, final Set<Function> functions) {
    this.hash = hash;
    this.name = name;
    this.functions = functions;
  }

  public void addClass(final Clazz clazz) {
    final Set<Clazz> newClasses = new HashSet<>(classes);
    newClasses.add(clazz);
    classes = Set.copyOf(newClasses);
  }

  public void addFunction(final Function function) {
    final Set<Function> newFunctions = new HashSet<>(functions);
    newFunctions.add(function);
    functions = Set.copyOf(newFunctions);
  }

  public Long getId() {
    return this.id;
  }

  public String getHash() {
    return this.hash;
  }

  public String getName() {
    return this.name;
  }

  public Set<Function> getFunctions() {
    return this.functions;
  }

  public void setHash(final String hash) {
    this.hash = hash;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(final Language language) {
    this.language = language;
  }

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(final String packageName) {
    this.packageName = packageName;
  }

  public String getLastEditor() {
    return lastEditor;
  }

  public void setLastEditor(final String lastEditor) {
    this.lastEditor = lastEditor;
  }

  public int getAddedLines() {
    return addedLines;
  }

  public void setAddedLines(final int addedLines) {
    this.addedLines = addedLines;
  }

  public int getModifiedLines() {
    return modifiedLines;
  }

  public void setModifiedLines(final int modifiedLines) {
    this.modifiedLines = modifiedLines;
  }

  public int getDeletedLines() {
    return deletedLines;
  }

  public void setDeletedLines(final int deletedLines) {
    this.deletedLines = deletedLines;
  }

  public boolean isHasFileData() {
    return hasFileData;
  }

  public void setHasFileData(final boolean hasFileData) {
    this.hasFileData = hasFileData;
  }

  public void addImportNames(final String importName) {
    final Set<String> newImportNames = new HashSet<>(importNames);
    newImportNames.add(importName);
    importNames = Set.copyOf(newImportNames);
  }

  public void addMetric(final String key, final Double value) {
    final Map<String, Double> newMetrics = new HashMap<>(metrics);
    newMetrics.put(key, value);
    metrics = Map.copyOf(newMetrics);
  }

  public Map<String, Double> getMetrics() {
    return this.metrics;
  }

  @Override
  public VisualizationObject toVisualizationObject() {
    return new Building(id.toString(), name,
        classes.stream().map(c -> c.getId().toString()).toList(),
        functions.stream().map(f -> f.getId().toString()).toList());
  }

  @Override
  public Stream<Visualizable> getVisualizableChildren() {
    return Stream.concat(classes.stream(), functions.stream());
  }
}
