package net.explorviz.persistence.api.v2.model;

/**
 * Represents a timestamp with span count information, as used by v2-API.
 *
 * @param epochNano Timestamp in nanoseconds since epoch
 * @param spanCount Number of spans at this timestamp
 */
public record TimestampDto(Number epochNano, Number spanCount) {}
