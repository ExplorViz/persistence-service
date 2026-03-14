package net.explorviz.persistence.api.v3.model.conversion;

import java.util.Map;
import java.util.stream.Stream;
import net.explorviz.persistence.api.v3.model.landscape.BuildingDto.BuildingConvertible;
import net.explorviz.persistence.api.v3.model.landscape.CityDto.CityConvertible;
import net.explorviz.persistence.api.v3.model.landscape.ClazzDto.ClassConvertible;
import net.explorviz.persistence.api.v3.model.landscape.DistrictDto.DistrictConvertible;
import net.explorviz.persistence.api.v3.model.landscape.FunctionDto.FunctionConvertible;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Clazz;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;

/**
 * Provides wrapper classes for turning OGM Application objects into FlatLandscape city models.
 */
public final class ApplicationToCityConverter {

  private ApplicationToCityConverter() {
  }

  public static CityConvertible convert(final Application ogmApp) {
    return new ApplicationWrapper(ogmApp);
  }

  private record ApplicationWrapper(Application ogmApp) implements CityConvertible {

    @Override
    public String getId() {
      return ogmApp.getId().toString();
    }

    @Override
    public String getName() {
      return ogmApp.getName();
    }

    @Override
    public Stream<DistrictConvertible> getDistricts() {
      return ogmApp.getRootDirectory().getSubdirectories().stream().map(DirectoryWrapper::new);
    }

    @Override
    public Stream<BuildingConvertible> getBuildings() {
      return ogmApp.getRootDirectory().getFileRevisions().stream().map(FileRevisionWrapper::new);
    }
  }


  private record DirectoryWrapper(Directory ogmDir) implements DistrictConvertible {

    @Override
    public String getId() {
      return ogmDir.getId().toString();
    }

    @Override
    public String getName() {
      return ogmDir.getName();
    }

    @Override
    public Stream<DistrictConvertible> getDistricts() {
      return ogmDir.getSubdirectories().stream().map(DirectoryWrapper::new);
    }

    @Override
    public Stream<BuildingConvertible> getBuildings() {
      return ogmDir.getFileRevisions().stream().map(FileRevisionWrapper::new);
    }
  }


  private record FileRevisionWrapper(FileRevision ogmFile) implements BuildingConvertible {

    @Override
    public String getId() {
      return ogmFile.getId().toString();
    }

    @Override
    public String getName() {
      return ogmFile.getName();
    }

    @Override
    public Stream<ClassConvertible> getClasses() {
      return ogmFile.getClasses().stream().map(ClassWrapper::new);
    }

    @Override
    public Stream<FunctionConvertible> getFunctions() {
      return ogmFile.getFunctions().stream().map(FunctionWrapper::new);
    }
  }


  private record ClassWrapper(Clazz ogmClass) implements ClassConvertible {

    @Override
    public String getId() {
      return ogmClass.getId().toString();
    }

    @Override
    public String getName() {
      return ogmClass.getName();
    }

    @Override
    public Stream<ClassConvertible> getInnerClasses() {
      return ogmClass.getInnerClasses().stream().map(ClassWrapper::new);
    }

    @Override
    public Stream<FunctionConvertible> getFunctions() {
      return ogmClass.getFunctions().stream().map(FunctionWrapper::new);
    }

    @Override
    public Map<String, Double> getMetrics() {
      return ogmClass.getMetrics();
    }
  }


  private record FunctionWrapper(Function ogmFunc) implements FunctionConvertible {

    @Override
    public String getId() {
      return ogmFunc.getId().toString();
    }

    @Override
    public String getName() {
      return ogmFunc.getName();
    }

    @Override
    public Map<String, Double> getMetrics() {
      return ogmFunc.getMetrics();
    }
  }
}
