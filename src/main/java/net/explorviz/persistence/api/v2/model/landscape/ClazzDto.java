package net.explorviz.persistence.api.v2.model.landscape;

import java.util.List;
import net.explorviz.persistence.ogm.FileRevision;

// Naming due to compatibility with v2 API
public record ClazzDto(String name, int level, List<FunctionDto> methods) {
  public ClazzDto(final FileRevision ogmFile, final int level) {
    this(ogmFile.getName(), level, ogmFile.getFunctions().stream().map(FunctionDto::new).toList());
  }
}
