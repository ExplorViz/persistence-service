package net.explorviz.persistence.ogm;

import java.util.ArrayList;
import java.util.List;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;

public class Field {
  @Id
  @GeneratedValue
  private Long id;

  private String name;

  private String type;

  private final List<String> modifiers;

  public Field() {
    this.modifiers = new ArrayList<>();
  }

  public Field(final String name, final String type, final List<String> modifiers) {
    this.name = name;
    this.type = type;
    this.modifiers = modifiers;
  }

  public String getName() {
    return this.name;
  }

  public String getType() {
    return this.type;
  }

  public List<String> getModifiers() {
    return this.modifiers;
  }
}
