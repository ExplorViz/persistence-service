package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.proto.StateDataRequest;
import net.explorviz.persistence.proto.StateDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class StateDataServiceTest {

  private static final long GRPC_AWAIT_SECONDS = 5;

  @GrpcClient
  StateDataService stateDataService;

  @Inject
  SessionFactory sessionFactory;

  @BeforeEach
  void cleanup() {
    Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  @Test
  void testGetStateData() {
    StateDataRequest stateDataRequest =
        StateDataRequest.newBuilder().setLandscapeToken("mytokenvalue").setRepositoryName("myrepo")
            .setBranchName("main").build();

    stateDataService.getStateData(stateDataRequest).await()
        .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

    Session session = sessionFactory.openSession();

    Landscape landscape = session.queryForObject(Landscape.class, """
        MATCH (l:Landscape {tokenId: $tokenId})
        RETURN l;
        """, Map.of("tokenId", "mytokenvalue"));

    Repository repository = session.queryForObject(Repository.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(r:Repository {name: $repoName})
        RETURN r;
        """, Map.of("tokenId", "mytokenvalue", "repoName", "myrepo"));

    Branch branch = session.queryForObject(Branch.class, """
        MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository {name: $repoName})
              -[:CONTAINS]->(b:Branch {name: $branchName})
        RETURN b;
        """, Map.of("tokenId", "mytokenvalue", "repoName", "myrepo", "branchName", "main"));

    assertNotNull(landscape);
    assertNotNull(repository);
    assertNotNull(branch);
  }
}
