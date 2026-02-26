package net.explorviz.persistence.api.v2.model.landscape;

import java.util.List;
import net.explorviz.persistence.ogm.FileRevision;

public record ClazzDto(
    String name,
    int level,
    List<FunctionDto> methods // Naming due to compatibility with v2 API
) {
  public ClazzDto(final FileRevision ogmFile, final int level) {
    this(ogmFile.getName(), level + 1,
        ogmFile.getFunctions().stream().map(FunctionDto::new).toList());
  }
}
