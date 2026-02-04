package net.explorviz.persistence.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import net.explorviz.persistence.api.model.TokenDTO;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.repository.LandscapeRepository;
import org.jboss.resteasy.reactive.RestPath;

/**
 * Resource for user-related data.
 */
@Path("/user")
public class UserResource {

  private static final String HARDCODED_OWNER_ID = "github|123456";

  /**
   * The repository for landscape data.
   */
  @Inject
  private LandscapeRepository landscapeRepository;

  /**
   * Returns all landscape tokens for a user.
   * Currently, this ignores the userId and returns all tokens in the database
   * with a hardcoded ownerId as per requirements.
   *
   * @param userId the id of the user
   * @return a list of tokens
   */
  @GET
  @Path("/{userId}/token")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TokenDTO> getUserTokens(@RestPath final String userId) {
    final Collection<Landscape> landscapes = landscapeRepository.findAllLandscapes();

    return landscapes.stream()
        .map(l -> new TokenDTO(l.getTokenId(), HARDCODED_OWNER_ID, l.getTokenId()))
        .collect(Collectors.toList());
  }
}
