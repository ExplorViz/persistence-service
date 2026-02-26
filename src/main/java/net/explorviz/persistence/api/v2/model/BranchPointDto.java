package net.explorviz.persistence.api.v2.model;

/**
 * Represents a point in a git branch from which another branch has emerged, as used by v2-API.
 *
 * @param name Origin branch name
 * @param commitId Commit hash from which the new branch emerged
 */
public record BranchPointDto(String name, String commitId) {
}
