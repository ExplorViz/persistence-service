package net.explorviz.persistence.api.v3.model;

import java.util.List;

/**
 * Represents the entire commit-tree of a git repository, where points of branching are explicitly
 * provided for the visualization.
 *
 * @param repositoryName Repository name
 * @param branches Branches of this repository
 */
public record CommitTreeDto(String repositoryName, List<BranchDto> branches) {}
