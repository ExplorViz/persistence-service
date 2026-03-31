package net.explorviz.persistence.ogm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Properties;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class FileRevision implements Comparable<FileRevision> {
  @Id @GeneratedValue private Long id;

  private String hash;

  private String name;

  private boolean hasFileData;

  private String language;

  private String packageName;

  private Set<String> importNames = new HashSet<>();

  @Properties private Map<String, Double> metrics = new HashMap<>();

  private String lastEditor;

  private int addedLines;

  private int modifiedLines;

  private int deletedLines;

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private final SortedSet<Clazz> classes = new TreeSet<>();

  @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
  private final SortedSet<Function> functions = new TreeSet<>();

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

  public void addClass(final Clazz clazz) {
    classes.add(clazz);
  }

  public void addFunction(final Function function) {
    functions.add(function);
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

  public SortedSet<Clazz> getClasses() {
    return new TreeSet<>(classes);
  }

  public SortedSet<Function> getFunctions() {
    return new TreeSet<>(functions);
  }

  public void setHash(final String hash) {
    this.hash = hash;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(final String language) {
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
  public int compareTo(final FileRevision other) {
    final int nameComparison = name.compareTo(other.name);

    if (nameComparison != 0) {
      return nameComparison;
    }

    if (hash != null & other.hash != null && hash.compareTo(other.hash) != 0) {
      return hash.compareTo(other.hash);
    }

    return id != null && other.id != null
        ? id.compareTo(other.id)
        : Integer.compare(System.identityHashCode(this), System.identityHashCode(other));
  }
}
