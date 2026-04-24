package net.explorviz.persistence.api.v3.model;

import java.util.Map;

/**
 * Represents aggregated communication, e.g. between files..
 *
 * @param id Unique identifier for the communication
 * @param name Name of the communication
 * @param sourceFileId ID of the source file
 * @param targetFileId ID of the target file
 * @param isBidirectional Whether the communication is bidirectional
 * @param metrics Metrics associated with the communication, e.g., requestCount
 */
public record CommunicationDto(
    String id,
    String name,
    String sourceFileId,
    String targetFileId,
    boolean isBidirectional,
    Map<String, Number> metrics) {}
