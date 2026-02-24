package net.explorviz.persistence.ogm;

import java.util.List;
import java.util.stream.Stream;
import net.explorviz.persistence.api.model.TypeOfAnalysis;
import net.explorviz.persistence.api.model.flat.City;
import net.explorviz.persistence.api.model.flat.VisualizationObject;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Application implements Visualizable {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  @Relationship(type = "HAS_ROOT", direction = Relationship.Direction.OUTGOING)
  private Directory rootDirectory;

  public Application() {
    // Empty constructor required by Neo4j OGM
  }

  public Application(final String name) {
    this.name = name;
  }

  public Directory getRootDirectory() {
    return rootDirectory;
  }

  public void setRootDirectory(final Directory rootDirectory) {
    this.rootDirectory = rootDirectory;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public VisualizationObject toVisualizationObject() {
    return new City(id.toString(), name, TypeOfAnalysis.DYNAMIC,
        List.of(rootDirectory.getId().toString()), List.of());
  }

  @Override
  public Stream<Visualizable> getVisualizableChildren() {
    return Stream.of(rootDirectory);
  }
}
