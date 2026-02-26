package net.explorviz.persistence.api.v2.model.landscape;

import java.util.List;
import net.explorviz.persistence.ogm.Directory;

public record PackageDto(
    String name,
    int level,
    List<PackageDto> subPackages,
    List<ClazzDto> classes
) {
  public PackageDto(final Directory ogmDir, final int level) {
    this(ogmDir.getName(), level,
        ogmDir.getSubdirectories().stream().map(d -> new PackageDto(d, level + 1)).toList(),
        ogmDir.getFileRevisions().stream().map(f -> new ClazzDto(f, level + 1)).toList());
  }
}
