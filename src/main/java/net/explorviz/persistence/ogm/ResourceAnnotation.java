package net.explorviz.persistence.ogm;

import java.time.Instant;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.annotation.typeconversion.DateLong;

@NodeEntity
public class ResourceAnnotation {
  @Id @GeneratedValue private Long id;

  @DateLong private Instant timestamp;
  private String externalId;
  private AnnotationType annotationType;

  private String label;
  private String mergeCommitHash;
  private String stateChange;

  @Relationship(type = "GENERATED_BY", direction = Relationship.Direction.OUTGOING)
  private ResourceVersion generatedResource;

  @Relationship(type = "USED", direction = Relationship.Direction.OUTGOING)
  private ResourceVersion usedResource;

  @Relationship(type = "CREATED_BY", direction = Relationship.Direction.INCOMING)
  private Contributor contributor;

  public Long getId() {
    return id;
  }

  public void setId(final Long id) {
    this.id = id;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getExternalId() {
    return externalId;
  }

  public void setExternalId(final String externalId) {
    this.externalId = externalId;
  }

  public AnnotationType getAnnotationType() {
    return annotationType;
  }

  public void setAnnotationType(final AnnotationType annotationType) {
    this.annotationType = annotationType;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(final String label) {
    this.label = label;
  }

  public String getMergeCommitHash() {
    return mergeCommitHash;
  }

  public void setMergeCommitHash(final String mergeCommitHash) {
    this.mergeCommitHash = mergeCommitHash;
  }

  public String getStateChange() {
    return stateChange;
  }

  public void setStateChange(final String stateChange) {
    this.stateChange = stateChange;
  }

  public ResourceVersion getGeneratedResource() {
    return generatedResource;
  }

  public void setGeneratedResource(final ResourceVersion generatedResource) {
    this.generatedResource = generatedResource;
  }

  public ResourceVersion getUsedResource() {
    return usedResource;
  }

  public void setUsedResource(final ResourceVersion resource) {
    this.usedResource = resource;
  }

  public Contributor getContributor() {
    return contributor;
  }

  public void setContributor(final Contributor contributor) {
    this.contributor = contributor;
  }
}
