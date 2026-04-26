package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Contributor;
import net.explorviz.persistence.repository.ContributorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
public class ContributorRepositoryTest {
  @Inject ContributorRepository contributorRepository;
  @Inject SessionFactory sessionFactory;

  private Session session;

  @BeforeEach
  void cleanup() {
    session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  @Test
  void testFindAnyContributor() {
    Session session = sessionFactory.openSession();
    // Setup test data
    session.query(
        """
        CREATE (b:Branch {name: 'repo1'})
        CREATE (c1:Contributor {name: 'Alice'})
        CREATE (c2:Contributor {name: 'Bob'})

        // Alice's first commit
        CREATE (c1)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)

        // Bob's commit
        CREATE (c2)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)

        // Alice's second commit
        CREATE (c1)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)
        """,
        new HashMap<>());

    Optional<Contributor> result = contributorRepository.findAnyContributor(session, "repo1");

    // Verify the results
    assertTrue(result.isPresent());
  }

  @Test
  void testFindContributorWithMostCommits() {
    Session session = sessionFactory.openSession();

    // Setup test data
    session.query(
        """
        CREATE (b:Branch {name: 'repo1'})
        CREATE (c1:Contributor {name: 'Alice'})
        CREATE (c2:Contributor {name: 'Bob'})

        // Alice's first commit
        CREATE (c1)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)

        // Bob's commit
        CREATE (c2)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)

        // Alice's second commit
        CREATE (c1)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)
        """,
        new HashMap<>());
    // Execute the method under test
    // var result = contributorRepository.findContributorWithMostCommits(session, "repo1");

    Optional<Contributor> result =
        contributorRepository.findContributorWithMostCommits(session, "repo1");
    // Verify the results
    assertTrue(result.isPresent());
    assertEquals("Alice", result.get().getName());
  }

  @Test
  void testCountCommitsPerContributor() {
    Session session = sessionFactory.openSession();
    // Setup test data
    session.query(
        """
        CREATE (b:Branch {name: 'repo1'})
        CREATE (c1:Contributor {name: 'Alice'})
        CREATE (c2:Contributor {name: 'Bob'})

        // Alice's first commit
        CREATE (c1)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)

        // Bob's commit
        CREATE (c2)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)

        // Alice's second commit
        CREATE (c1)-[:AUTHORED]->(:Commit)-[:IN_BRANCH]->(b)
        """,
        new HashMap<>());

    Map<String, Long> result = contributorRepository.countCommitsPerContributor(session, "repo1");

    // Verify the results
    assertEquals(2L, result.get("Alice"));
    assertEquals(1L, result.get("Bob"));
  }
}
