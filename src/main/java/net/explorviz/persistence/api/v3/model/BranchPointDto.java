package net.explorviz.persistence.api.v3.model;

/**
 * Represents a point in a git branch from which another branch has emerged.
 *
 * @param originBranchName Name of the branch from which the new branch emerged
 * @param originCommitHash Commit hash from which the new branch emerged
 */
public record BranchPointDto(String originBranchName, String originCommitHash) {
}
