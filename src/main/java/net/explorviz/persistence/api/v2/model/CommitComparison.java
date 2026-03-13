package net.explorviz.persistence.api.v2.model;

import java.util.List;

public record CommitComparison(List<String> modified, List<String> added, List<String> deleted,
                               List<String> addedPackages, List<String> deletedPackages,
                               List<EntityMetricsComparison> metrics) {
}
