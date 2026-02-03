package net.explorviz.persistence.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.explorviz.persistence.api.model.ApplicationCommitTree;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.repository.RepositoryRepository;
import org.jboss.resteasy.reactive.RestPath;

@Path("/v2/code")
public class CodeResource {

  @Inject
  RepositoryRepository repositoryRepository;

  @GET
  @Path("/applications/{landscapeToken}")
  @Produces(MediaType.APPLICATION_JSON)
  public List<String> getApplications(@RestPath final String landscapeToken) {
    return repositoryRepository.findRepositoriesByLandscapeToken(landscapeToken)
        .stream()
        .map(Repository::getName)
        .map(this::hashId)
        .collect(Collectors.toList());
  }

  @GET
  @Path("/commit-tree/{landscapeToken}/{appId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, ApplicationCommitTree> getCommitTree(@RestPath final String landscapeToken,
      @RestPath final String appId) {

    String repoName = null;
    for (Repository r : repositoryRepository.findRepositoriesByLandscapeToken(landscapeToken)) {
      if (hashId(r.getName()).equals(appId)) {
        repoName = r.getName();
        break;
      }
    }

    if (repoName == null) {
      throw new jakarta.ws.rs.NotFoundException("Application not found for id: " + appId);
    }

    final List<net.explorviz.persistence.api.model.BranchTree> branches = new java.util.ArrayList<>();
    final Iterable<Map<String, Object>> data = repositoryRepository.findCommitTreeData(repoName, landscapeToken);

    for (Map<String, Object> row : data) {
      String branchName = (String) row.get("branchName");
      if (branchName == null) {
        branchName = "main"; // Default if no branch found
      }
      
      Object commitsObj = row.get("commitHashes");
      List<String> commits;
      if (commitsObj instanceof String[]) {
        commits = java.util.Arrays.asList((String[]) commitsObj);
      } else if (commitsObj instanceof List) {
        commits = (List<String>) commitsObj;
      } else {
        commits = new java.util.ArrayList<>();
      }
      
      branches.add(new net.explorviz.persistence.api.model.BranchTree(branchName, commits, 
          new net.explorviz.persistence.api.model.BranchPoint("NONE", "")));
    }

    return java.util.Map.of(appId, 
        new net.explorviz.persistence.api.model.ApplicationCommitTree(repoName, branches));
  }

  private String hashId(String val) {
    return UUID.nameUUIDFromBytes(val.getBytes(StandardCharsets.UTF_8)).toString();
  }
}
