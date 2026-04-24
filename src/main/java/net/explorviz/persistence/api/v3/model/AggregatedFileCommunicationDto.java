package net.explorviz.persistence.api.v3.model;

import java.util.List;
import java.util.Map;

/**
 * Represents aggregated communication between files including metric summaries.
 *
 * @param metrics Summary of metrics across all communications
 * @param communications List of individual communication edges
 */
public record AggregatedFileCommunicationDto(
    Map<String, MetricSummaryDto> metrics, List<CommunicationDto> communications) {}
