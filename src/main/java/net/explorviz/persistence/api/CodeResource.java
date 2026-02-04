package net.explorviz.persistence.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import net.explorviz.persistence.api.model.ApplicationCommitTree;
import net.explorviz.persistence.api.model.BranchPoint;
import net.explorviz.persistence.api.model.BranchTree;
import net.explorviz.persistence.api.model.FlatLandscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.repository.RepositoryRepository;
import net.explorviz.persistence.service.StructureService;
import org.jboss.resteasy.reactive.RestPath;

@Path("/v2/code")
public class CodeResource {

  @Inject
  RepositoryRepository repositoryRepository;

  @Inject
  StructureService structureService;

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
  public ApplicationCommitTree getCommitTree(@RestPath final String landscapeToken,
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

    final List<BranchTree> branches = new ArrayList<>();
    final Iterable<Map<String, Object>> data = repositoryRepository.findCommitTreeData(repoName, landscapeToken);

    for (Map<String, Object> row : data) {
      String branchName = (String) row.get("branchName");
      if (branchName == null) {
        branchName = "main"; // Default if no branch found
      }
      
      Object commitsObj = row.get("commitHashes");
      List<String> commits;
      if (commitsObj instanceof String[]) {
        commits = Arrays.asList((String[]) commitsObj);
      } else if (commitsObj instanceof List) {
        commits = (List<String>) commitsObj;
      } else {
        commits = new ArrayList<>();
      }
      
      branches.add(new BranchTree(branchName, commits, 
          new BranchPoint("NONE", "")));
    }

    return new ApplicationCommitTree(repoName, branches);
  }

  @GET
  @Path("/structure/{landscapeToken}/{appId}/{commitId}")
  @Produces(MediaType.APPLICATION_JSON)
  public FlatLandscape getStructure(@RestPath final String landscapeToken,
      @RestPath final String appId, @RestPath final String commitId) {
    Optional<FlatLandscape> landscape = structureService.getLandscape(landscapeToken, commitId);
    if (landscape.isEmpty()) {
      throw new jakarta.ws.rs.NotFoundException("Structure not found for commit: " + commitId);
    }
    return landscape.get();
  }

  @GET
  @Path("/structure/{landscapeToken}/{appId}/{commitId1}-{commitId2}")
  @Produces(MediaType.APPLICATION_JSON)
  public FlatLandscape getComparisonStructure(@RestPath final String landscapeToken,
      @RestPath final String appId, @RestPath final String commitId1,
      @RestPath final String commitId2) {
    Optional<FlatLandscape> landscape = structureService.getLandscape(landscapeToken, List.of(commitId1, commitId2));
    if (landscape.isEmpty()) {
      throw new jakarta.ws.rs.NotFoundException("Structure not found for commits: " + commitId1 + ", " + commitId2);
    }
    return landscape.get();
  }

  private String hashId(String val) {
    return UUID.nameUUIDFromBytes(val.getBytes(StandardCharsets.UTF_8)).toString();
  }
}
