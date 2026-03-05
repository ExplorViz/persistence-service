package net.explorviz.persistence.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for a landscape token.
 */
public class TokenDTO {
  /**
   * The token value.
   */
  public String value;
  /**
   * The owner ID.
   */
  public String ownerId;
  /**
   * The alias of the token.
   */
  public String alias;
  /**
   * Creation time as epoch milliseconds.
   */
  public long created;
  /**
   * The IDs of users this token is shared with.
   */
  public List<String> sharedUsersIds = new ArrayList<>();

  public TokenDTO() {
    // Empty constructor required for Jackson
  }

  public TokenDTO(final String value, final String ownerId, final String alias) {
    this.value = value;
    this.ownerId = ownerId;
    this.alias = alias;
  }

  public TokenDTO(final String value, final String ownerId, final String alias,
      final long created) {
    this.value = value;
    this.ownerId = ownerId;
    this.alias = alias;
    this.created = created;
  }
}
