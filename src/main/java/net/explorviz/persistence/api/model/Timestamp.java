package net.explorviz.persistence.api.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Timestamp {
  public long epochNano;
  public int spanCount;

  public Timestamp() {}

  public Timestamp(long epochNano, int spanCount) {
    this.epochNano = epochNano;
    this.spanCount = spanCount;
  }
}
