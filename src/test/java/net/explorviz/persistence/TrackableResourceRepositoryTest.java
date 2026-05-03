package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import net.explorviz.persistence.ogm.AnnotationType;
import net.explorviz.persistence.ogm.Contributor;
import net.explorviz.persistence.ogm.Issue;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.PullRequest;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.ogm.ResourceAnnotation;
import net.explorviz.persistence.ogm.ResourceState;
import net.explorviz.persistence.ogm.ResourceVersion;
import net.explorviz.persistence.repository.TrackableResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class TrackableResourceRepositoryTest {

  @Inject TrackableResourceRepository trackableResourceRepository;

  @Inject SessionFactory sessionFactory;

  @BeforeEach
  void cleanup() {
    final Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  @Test
  void testGetOrCreateCreatesNewWhenNotFound() {
    final Session session = sessionFactory.openSession();
    final String tokenId = "token-1";
    final String repoName = "repo-1";

    // Test Issue creation
    Issue newIssue =
        trackableResourceRepository.getOrCreate(session, Issue.class, 100, repoName, tokenId);
    assertNotNull(newIssue);
    assertEquals(100, newIssue.getNumber());

    // Test PR creation
    PullRequest newPr =
        trackableResourceRepository.getOrCreate(session, PullRequest.class, 101, repoName, tokenId);
    assertNotNull(newPr);
    assertEquals(101, newPr.getNumber());
  }

  @Test
  void testFindByNumberWhenExists() {
    final Session session = sessionFactory.openSession();
    final String tokenId = "token-1";
    final String repoName = "repo-1";

    // Setup graph
    Landscape landscape = new Landscape(tokenId);
    Repository repository = new Repository(repoName);

    Issue issue = new Issue();
    issue.setNumber(200);
    issue.setTitle("Test Issue");

    // We do a raw cypher save to link them since Repository doesn't have an addIssue method in our
    // model context,
    // or we can save them and manually attach relationships using cypher for testing the MATCH
    // query.
    session.save(landscape);
    session.save(repository);
    session.save(issue);

    session.query(
        """
        MATCH (l:Landscape {tokenId: $tokenId}), (r:Repository {name: $repoName}), (i:Issue {number: $number})
        CREATE (l)-[:CONTAINS]->(r)-[:CONTAINS]->(i)
        """,
        java.util.Map.of("tokenId", tokenId, "repoName", repoName, "number", 200));

    // Call findByNumber
    Optional<Issue> foundIssue =
        trackableResourceRepository.findByNumber(session, Issue.class, 200, repoName, tokenId);

    assertTrue(foundIssue.isPresent());
    assertEquals(200, foundIssue.get().getNumber());
    assertEquals("Test Issue", foundIssue.get().getTitle());
  }

  @Test
  void testFindByNumberWrongRepo() {
    final Session session = sessionFactory.openSession();

    Landscape landscape = new Landscape("token-1");
    Repository repository = new Repository("repo-1");
    Issue issue = new Issue();
    issue.setNumber(300);

    session.save(landscape);
    session.save(repository);
    session.save(issue);

    session.query(
        """
        MATCH (l:Landscape {tokenId: $tokenId}), (r:Repository {name: $repoName}), (i:Issue {number: $number})
        CREATE (l)-[:CONTAINS]->(r)-[:CONTAINS]->(i)
        """,
        java.util.Map.of("tokenId", "token-1", "repoName", "repo-1", "number", 300));

    // Call finding on wrong repo
    Optional<Issue> notFoundIssue =
        trackableResourceRepository.findByNumber(session, Issue.class, 300, "repo-2", "token-1");
    assertFalse(notFoundIssue.isPresent());
  }

  //  @Test
  //  void testAddEventToResource() {
  //
  //    final Session session = sessionFactory.openSession();
  //    final String tokenId = "token-1";
  //    final String repoName = "repo-1";
  //
  //    // Setup graph
  //    Landscape landscape = new Landscape(tokenId);
  //    Repository repository = new Repository(repoName);
  //
  //    Issue issue = new Issue();
  //    issue.setNumber(200);
  //    issue.setTitle("Test Issue");
  //
  //    // We do a raw cypher save to link them since Repository doesn't have an addIssue method in
  // our
  //    // model context,
  //    // or we can save them and manually attach relationships using cypher for testing the MATCH
  //    // query.
  //    session.save(landscape);
  //    session.save(repository);
  //    session.save(issue);
  //
  //    // TODO: Maybe the relationships should be created in the repository class already
  //    session.query(
  //        """
  //        MATCH (l:Landscape {tokenId: $tokenId}), (r:Repository {name: $repoName}), (i:Issue
  // {number: $number})
  //        CREATE (l)-[:CONTAINS]->(r)-[:CONTAINS]->(i)
  //        """,
  //        java.util.Map.of("tokenId", tokenId, "repoName", repoName, "number", 200));
  //
  //    CreatedEvent event =
  //        new CreatedEvent(Instant.now(), new Contributor("test"), "external-1", issue);
  //    trackableResourceRepository.addEventToResource(
  //        session, Issue.class, 200, "repo-1", "token-1", event);
  //
  //    session.save(event);
  //
  //    Optional<Issue> foundIssue =
  //        trackableResourceRepository.findByNumber(session, Issue.class, 200, repoName, tokenId);
  //    assertTrue(foundIssue.isPresent());
  //    Issue updatedIssue = foundIssue.get();
  //    System.out.println("Events: " + updatedIssue.getEvents());
  //    assertNotNull(updatedIssue.getEvents());
  //    assertTrue(updatedIssue.getEvents().stream().anyMatch(e -> e instanceof CreatedEvent));
  //  }

  @Test
  void testAddAnnotationAndVersion() {
    final Session session = sessionFactory.openSession();
    final String tokenId = "token-1";
    final String repoName = "repo-1";
    Landscape landscape = new Landscape(tokenId);
    Repository repository = new Repository(repoName);

    Contributor contributor = new Contributor("IssueCreator");
    Issue issue = new Issue();
    issue.setNumber(200);
    issue.setTitle("Test Issue");
    session.save(landscape);
    session.save(repository);
    session.save(issue);

    session.query(
        """
        MATCH (l:Landscape {tokenId: $tokenId}), (r:Repository {name: $repoName}), (i:Issue {number: $number})
        CREATE (l)-[:CONTAINS]->(r)-[:CONTAINS]->(i)
        """,
        java.util.Map.of("tokenId", tokenId, "repoName", repoName, "number", 200));

    // Create new Issue Version on creation with creation annotation
    Contributor creator = new Contributor("annotator");
    ResourceVersion creationVersion = new ResourceVersion();
    creationVersion.setCreationDate(Instant.now());
    creationVersion.setCreatedBy(creator);
    creationVersion.setTitle("Test issue");
    creationVersion.setState(ResourceState.OPEN);
    creationVersion.setExternalId("external-1");

    ResourceAnnotation creationAnnotation =
        trackableResourceRepository.addAnnotationAndVersion(
            session,
            Issue.class,
            200,
            repoName,
            tokenId,
            "external-1",
            Instant.now(),
            AnnotationType.CREATE,
            creator,
            creationVersion);
    assertNotNull(creationAnnotation);

    // find issue and test that versions are present
    Optional<Issue> foundIssue =
        trackableResourceRepository.findByNumber(session, Issue.class, 200, repoName, tokenId);
    assertTrue(foundIssue.isPresent());
    Issue updatedIssue = foundIssue.get();
    assertNotNull(updatedIssue.getVersions());
    assertEquals(1, updatedIssue.getVersions().size());

    // test getCurrentVersion
    ResourceVersion currentVersion = updatedIssue.getCurrentVersion();
    assertNotNull(currentVersion);
    assertEquals(creationVersion.getCreationDate(), currentVersion.getCreationDate());
    assertEquals(creationVersion.getCreatedBy(), creator);

    // Create another annotation
    Contributor closer = new Contributor("closer");
    ResourceVersion closedVersion = new ResourceVersion();

    closedVersion.setTitle(currentVersion.getTitle());
    closedVersion.setState(ResourceState.CLOSED);
    closedVersion.setExternalId(currentVersion.getExternalId());
    closedVersion.setCreationDate(Instant.now());
    closedVersion.setCreatedBy(closer);

    ResourceAnnotation closedAnnotation =
        trackableResourceRepository.addAnnotationAndVersion(
            session,
            Issue.class,
            200,
            repoName,
            tokenId,
            "external-1",
            Instant.now(),
            AnnotationType.CLOSE,
            closer,
            closedVersion);

    assertNotNull(closedAnnotation);
    assertNotNull(closedAnnotation.getUsedResource());
    assertEquals(ResourceState.OPEN, closedAnnotation.getUsedResource().getState());
    assertNotNull(closedAnnotation.getGeneratedResource());
    assertEquals(ResourceState.CLOSED, closedAnnotation.getGeneratedResource().getState());
    assertEquals(closer, closedAnnotation.getContributor());

    // get updated issue
    Optional<Issue> foundIssueAfterClose =
        trackableResourceRepository.findByNumber(session, Issue.class, 200, repoName, tokenId);
    assertTrue(foundIssueAfterClose.isPresent());
    Issue closedIssueAfterClose = foundIssueAfterClose.get();
    assertEquals(2, closedIssueAfterClose.getVersions().size());

    // test latest version again and walk chain to last
    ResourceVersion latestVersion = closedIssueAfterClose.getCurrentVersion();
    assertNotNull(latestVersion);
    assertEquals(ResourceState.CLOSED, latestVersion.getState());
    assertEquals(ResourceState.OPEN, latestVersion.getGeneratedBy().getUsedResource().getState());

    // test derivation
    assertNotNull(latestVersion.getDerivedFrom());
    assertEquals(ResourceState.OPEN, latestVersion.getDerivedFrom().getState());
  }
}
