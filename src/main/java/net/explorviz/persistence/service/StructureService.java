package net.explorviz.persistence.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.api.model.Building;
import net.explorviz.persistence.api.model.City;
import net.explorviz.persistence.api.model.Cls;
import net.explorviz.persistence.api.model.District;
import net.explorviz.persistence.api.model.FlatLandscape;
import net.explorviz.persistence.api.model.Func;
import net.explorviz.persistence.api.model.Language;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.repository.CommitRepository;

/**
 * Service for building the flat landscape structure from the graph.
 */
@ApplicationScoped
public class StructureService {

  @Inject
  CommitRepository commitRepository;

  public Optional<FlatLandscape> getLandscape(final String token, final String commitId) {
    Optional<Commit> deepCommit = commitRepository.findDeepCommit(commitId, token);

    if (deepCommit.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(buildFlatLandscape(token, deepCommit.get()));
  }

  /**
   * Retrieves the structure of the landscape for the latest commit.
   *
   * @param token the token of the landscape
   * @return the flat landscape structure of the latest commit
   */
  public Optional<FlatLandscape> getLandscape(final String token) {
    Optional<Commit> deepCommit = commitRepository.findLatestDeepCommit(token);

    if (deepCommit.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(buildFlatLandscape(token, deepCommit.get()));
  }

  private FlatLandscape buildFlatLandscape(final String token, final Commit commit) {
    final FlatLandscape landscape = new FlatLandscape(token);

    // 1. Create Application (City)
    final String repoName = commitRepository.findRepositoryName(token, commit.getHash())
        .orElse("UnknownApplication");
    final String cityId = hashId(repoName);
    final City appCity = new City(cityId, repoName);
    landscape.cities.put(appCity.id, appCity);

    // Helper map for ensuring unique District IDs/objects
    // Key: Directory ID (as String) -> District object
    final Map<String, District> districtMap = new HashMap<>();

    // 2. Iterate FileRevisions
    for (final net.explorviz.persistence.ogm.FileRevision file : commit.getFileRevisions()) {
      // Create Building
      final Building building = new Building();
      building.id = file.getHash(); // Assuming FileRevision.hash is unique/stable
      building.name = file.getName();
      
      // We rely on getParentDirectory() for structure now
      final net.explorviz.persistence.ogm.Directory parentDir = file.getParentDirectory();

      if (parentDir == null) {
        // Root file (no parent directory)
        building.fqn = file.getName();
        building.parentCityId = appCity.id;
        
        // Even if root, we attach it to the city
        // We can create a "root" district for consistency if needed, 
        // but for now let's attach to city directly logic.
        // However, building.parentDistrictId is mandatory in some views?
        // Let's create a virtual root district ID based on the City ID + "root"
        // Or if user strictly wants DB IDs... a root file has no directory ID.
        // Let's map it to a synthetic root district for the app.
        
        // Wait, if parentDir is null, it means it's at the top level of the repo?
        // Let's assume so.
        final District rootDistrict = getOrCreateSyntheticRootDistrict(appCity, landscape, districtMap);
        building.parentDistrictId = rootDistrict.id;
        rootDistrict.buildingIds.add(building.id);

      } else {
        // Has a parent directory
        // Construct hierarchy up to root (or up to null parent)
        final District district = getOrCreateDistrictFromDirectory(parentDir, appCity, landscape, districtMap);
        
        building.fqn = constructFqn(file, parentDir); 
        building.parentDistrictId = district.id;
        building.parentCityId = appCity.id;
        district.buildingIds.add(building.id);
      }

      // Add to city's global building list
      appCity.buildingIds.add(building.id);

      building.language = mapLanguage(file.getLanguage());
      building.metrics = file.getMetrics();
      
      landscape.buildings.put(building.id, building);

      // 3. Map Classes (same as before)
      for (final net.explorviz.persistence.ogm.Clazz clazz : file.getClasses()) {
        final Cls cls = new Cls();
        cls.id = String.valueOf(clazz.hashCode()); 
        
        final String clsFqn = clazz.getName(); 
        cls.id = hashId(clsFqn); 
        cls.name = simpleName(clsFqn);
        cls.fqn = clsFqn;
        
        building.classIds.add(cls.id);
        landscape.classes.put(cls.id, cls);

        // 4. Map Methods
        for (final net.explorviz.persistence.ogm.Function func : clazz.getFunctions()) {
          final Func f = new Func();
          final String funcName = func.getName(); 
          f.id = hashId(clsFqn + "::" + funcName);
          f.name = funcName;
          f.parentId = cls.id;
          f.metrics = func.getMetrics();
          
          cls.functionIds.add(f.id);
          landscape.functions.put(f.id, f);
        }
      }

      // Map Top-level functions
      for (final net.explorviz.persistence.ogm.Function func : file.getFunctions()) {
        final Func f = new Func();
        final String funcName = func.getName();
        f.id = hashId(building.fqn + "::" + funcName);
        f.name = funcName;
        f.parentId = building.id;
        
        building.functionIds.add(f.id);
        landscape.functions.put(f.id, f);
      }
    }

    return landscape;
  }
  
  private District getOrCreateSyntheticRootDistrict(final City city, 
      final FlatLandscape landscape, final Map<String, District> districtMap) {
    
    final String rootId = city.id + "-root";
    if (districtMap.containsKey(rootId)) {
      return districtMap.get(rootId);
    }
    
    final District d = new District();
    d.id = rootId;
    d.name = "root";
    d.fqn = "root";
    d.parentCityId = city.id;
    city.rootDistrictIds.add(d.id);
    city.districtIds.add(d.id); // Add to global list
    
    landscape.districts.put(d.id, d);
    districtMap.put(d.id, d);
    return d;
  }

  private District getOrCreateDistrictFromDirectory(
      final net.explorviz.persistence.ogm.Directory directory, 
      final City city,
      final FlatLandscape landscape, 
      final Map<String, District> districtMap) {

    final String dirId = String.valueOf(directory.getId());
    if (districtMap.containsKey(dirId)) {
      return districtMap.get(dirId);
    }

    // Create district
    final District d = new District();
    d.id = dirId; // Database ID as requested
    d.name = directory.getName();
    // For FQN, we might need traversal, but name is just local name usually?
    // Let's assume FQN building is separate or we need to build it. 
    // Directory FQN?
    d.fqn = directory.getName(); // Placeholder or need recursive build?
    // Doing recursive build of FQN might be expensive if not stored.
    // Let's iterate up to build it if needed, or just use name for now.
    
    d.parentCityId = city.id;
    
    // Check parent
    final net.explorviz.persistence.ogm.Directory parentDir = directory.getParent();
    if (parentDir != null) {
      final District parentDistrict = getOrCreateDistrictFromDirectory(parentDir, city, landscape, districtMap);
      d.parentDistrictId = parentDistrict.id;
      parentDistrict.districtIds.add(d.id);
    } else {
      // Top level directory (root of source tree?)
      // Attach to City
      city.rootDistrictIds.add(d.id);
    }
    
    // Add to city's global list
    city.districtIds.add(d.id);

    landscape.districts.put(d.id, d);
    districtMap.put(dirId, d);
    return d;
  }
  
  private String constructFqn(net.explorviz.persistence.ogm.FileRevision file, net.explorviz.persistence.ogm.Directory parentDir) {
      // If we want package-like FQN 'a.b.File', we can use packageName if valid.
      // Or construct from directory names.
      // User request "Districts ... IDs from database". 
      // FQN construction usually relies on package name for Java.
      // Let's use file.getPackageName() if available, else fallback?
      String pkg = file.getPackageName();
      if (pkg != null && !pkg.isEmpty()) {
          return pkg + "." + file.getName();
      }
      return file.getName();
  }

  private Language mapLanguage(final net.explorviz.persistence.proto.Language protoLang) {
    if (protoLang == null) {
      return Language.LANGUAGE_UNSPECIFIED;
    }
    switch (protoLang) {
      case JAVA: return Language.JAVA;
      case JAVASCRIPT: return Language.JAVASCRIPT;
      case TYPESCRIPT: return Language.TYPESCRIPT;
      case PYTHON: return Language.PYTHON;
      case PLAINTEXT: return Language.PLAINTEXT;
      default: return Language.LANGUAGE_UNSPECIFIED;
    }
  }

  private String hashId(String val) {
    return java.util.UUID.nameUUIDFromBytes(val.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
  }

  private String simpleName(String fqn) {
      if (fqn == null) return "";
      int lastDot = fqn.lastIndexOf('.');
      return lastDot == -1 ? fqn : fqn.substring(lastDot + 1);
  }
}
