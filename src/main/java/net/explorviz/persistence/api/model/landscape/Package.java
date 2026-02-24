package net.explorviz.persistence.api.model.landscape;

import java.util.List;
import net.explorviz.persistence.ogm.Directory;

public record Package(
    String name,
    int level,
    List<Package> subPackages,
    List<Clazz> classes
) {
  public Package(final Directory ogmDir, int level) {
    this(ogmDir.getName(), level,
        ogmDir.getSubdirectories().stream().map(d -> new Package(d, level + 1)).toList(),
        ogmDir.getFileRevisions().stream().map(f -> new Clazz(f, level + 1)).toList());
  }
}
