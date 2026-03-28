package net.explorviz.persistence.api.v2.model;

import java.util.Set;

/**
 * Represents a git branch as used by the frontend in the v2-API.
 *
 * @param name Name of the branch
 * @param commits Hashes of commits in this branch
 * @param branchPoint Commit of different branch where this branch originated from
 */
public record BranchDto(String name, Set<String> commits, BranchPointDto branchPoint) {}
