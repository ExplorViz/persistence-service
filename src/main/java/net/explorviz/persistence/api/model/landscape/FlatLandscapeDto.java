package net.explorviz.persistence.api.model.landscape;

import java.util.Map;
import java.util.Objects;

/**
 * The central structure data object exchanged with the frontend. Captures the structural state of a
 * specific landscape under some condition, i.e. for some particular git commit or containing only
 * dynamic data from a particular timestamp.
 * 
 * <p>Uses a flat data model, meaning that visualization
 * objects do not contain their children directly (leading to deeply nested objects), instead
 * referencing them only via their ID. The lookup can then be performed using the maps provided by
 * this object.
 *
 * @param landscapeToken String identifier of the visualization landscape
 * @param cities         All city model objects in the landscape, indexed by their ID
 * @param districts      All district model objects in the landscape, indexed by their ID
 * @param buildings      All building model objects in the landscape, indexed by their ID
 * @param classes        All class model objects in the landscape, indexed by their ID
 * @param functions      All function model objects in the landscape, indexed by their ID
 */
public record FlatLandscapeDto(String landscapeToken, Map<String, CityDto> cities,
                               Map<String, DistrictDto> districts,
                               Map<String, BuildingDto> buildings, Map<String, ClazzDto> classes,
                               Map<String, FunctionDto> functions) {
  public FlatLandscapeDto {
    Objects.requireNonNull(landscapeToken);
    Objects.requireNonNull(cities);
    Objects.requireNonNull(districts);
    Objects.requireNonNull(buildings);
    Objects.requireNonNull(classes);
    Objects.requireNonNull(functions);
  }
}
