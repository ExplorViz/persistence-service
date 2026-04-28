package net.explorviz.persistence.ogm.events;

import net.explorviz.persistence.ogm.ResourceEvent;
import org.neo4j.ogm.annotation.NodeEntity;

@NodeEntity
public class LabeledEvent extends ResourceEvent {
  private String label;

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }
}
