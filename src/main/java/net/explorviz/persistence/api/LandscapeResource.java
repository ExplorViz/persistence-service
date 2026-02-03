package net.explorviz.persistence.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Optional;
import net.explorviz.persistence.api.model.FlatLandscape;
import net.explorviz.persistence.service.StructureService;
import org.jboss.resteasy.reactive.RestPath;

/**
 * Resource for accessing landscape structure data.
 */
@Path("/v2/landscapes/{landscapeToken}/structure")
public class LandscapeResource {

  @Inject
  StructureService structureService;

  /**
   * Returns the structure of the landscape for a given commit.
   *
   * @param landscapeToken the token of the landscape
   * @param commitId the id of the commit
   * @return the flat landscape structure
   */
  @GET
  @Path("/{commitId}")
  @Produces(MediaType.APPLICATION_JSON)
  public FlatLandscape getStructure(@RestPath final String landscapeToken,
      @RestPath final String commitId) {
    
    Optional<FlatLandscape> landscape = structureService.getLandscape(landscapeToken, commitId);
    
    if (landscape.isEmpty()) {
       throw new NotFoundException("Structure not found for commit: " + commitId);
    }
    
    return landscape.get();
  }

  /**
   * Returns the structure of the latest landscape (latest commit).
   *
   * @param landscapeToken the token of the landscape
   * @return the flat landscape structure of the latest commit
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public FlatLandscape getLatestStructure(@RestPath final String landscapeToken) {
    
    Optional<FlatLandscape> landscape = structureService.getLandscape(landscapeToken);
    
    if (landscape.isEmpty()) {
       throw new NotFoundException("Structure not found for latest commit of landscape: " + landscapeToken);
    }
    
    return landscape.get();
  }
}
