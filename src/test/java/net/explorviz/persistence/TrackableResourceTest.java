package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.explorviz.persistence.ogm.Issue;
import net.explorviz.persistence.ogm.PullRequest;
import net.explorviz.persistence.ogm.TrackableResource;
import org.junit.jupiter.api.Test;

class TrackableResourceTest {

  @Test
  void testIssueProperties() {
    Issue issue = new Issue();
    issue.setNumber(1);
    issue.setTitle("Bug in login");
    issue.setState("open");
    issue.addLabel("bug");

    assertEquals(1, issue.getNumber());
    assertEquals("Bug in login", issue.getTitle());
    assertEquals("open", issue.getState());
    assertNotNull(issue.getLabels());
    assertTrue(issue.getLabels().contains("bug"));
  }

  @Test
  void testPullRequestProperties() {
    PullRequest pr = new PullRequest();
    pr.setNumber(42);
    pr.setTitle("Fix login bug");
    pr.setState("merged");
    pr.setLabels(Set.of("enhancement", "fix"));

    assertEquals(42, pr.getNumber());
    assertEquals("Fix login bug", pr.getTitle());
    assertEquals("merged", pr.getState());
    assertEquals(2, pr.getLabels().size());
    assertTrue(pr.getLabels().contains("fix"));
  }

  @Test
  void testTrackableResourceConstructor() {
    // We can instantiate an anonymous class extending TrackableResource to test the constructor
    TrackableResource customResource =
        new TrackableResource(100, "Custom TR", "closed", Set.of("custom")) {};

    assertEquals(100, customResource.getNumber());
    assertEquals("Custom TR", customResource.getTitle());
    assertEquals("closed", customResource.getState());
    assertTrue(customResource.getLabels().contains("custom"));
  }
}
