package net.explorviz.persistence.api.model.landscape;

import java.util.List;
import net.explorviz.persistence.ogm.FileRevision;

public record Clazz(
    String name,
    int level,
    List<Method> methods
) {
  public Clazz(final FileRevision ogmFile, final int level) {
    this(ogmFile.getName(), level + 1, ogmFile.getFunctions().stream().map(Method::new).toList());
  }
}
