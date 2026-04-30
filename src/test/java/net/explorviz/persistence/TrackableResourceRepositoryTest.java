package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import net.explorviz.persistence.ogm.Contributor;
import net.explorviz.persistence.ogm.Issue;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.PullRequest;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.ogm.events.*;
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

  @Test
  void testAddEventToResource() {

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

    // TODO: Maybe the relationships should be created in the repository class already
    session.query(
        """
        MATCH (l:Landscape {tokenId: $tokenId}), (r:Repository {name: $repoName}), (i:Issue {number: $number})
        CREATE (l)-[:CONTAINS]->(r)-[:CONTAINS]->(i)
        """,
        java.util.Map.of("tokenId", tokenId, "repoName", repoName, "number", 200));

    CreatedEvent event =
        new CreatedEvent(Instant.now(), new Contributor("test"), "external-1", issue);
    trackableResourceRepository.addEventToResource(
        session, Issue.class, 200, "repo-1", "token-1", event);

    session.save(event);

    Optional<Issue> foundIssue =
        trackableResourceRepository.findByNumber(session, Issue.class, 200, repoName, tokenId);
    assertTrue(foundIssue.isPresent());
    Issue updatedIssue = foundIssue.get();
    System.out.println("Events: " + updatedIssue.getEvents());
    assertNotNull(updatedIssue.getEvents());
    assertTrue(updatedIssue.getEvents().stream().anyMatch(e -> e instanceof CreatedEvent));
  }
}
