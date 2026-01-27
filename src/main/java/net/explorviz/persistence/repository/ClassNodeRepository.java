package net.explorviz.persistence.repository;

import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Clazz;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

public class ClassNodeRepository {

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Clazz> findClassByLandscapeTokenAndRepositoryAndFileHash(
      final Session session, final String tokenId, final String repoName, final String fileHash) {
    return Optional.ofNullable(session.queryForObject(Clazz.class, """
        MATCH (:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(:Repository {name: $repoName})
          -[:CONTAINS]->(:Commit)
          -[:CONTAINS]->(f:FileRevision {hash: $fileHash})
          -[:CONTAINS]->(cl:ClassNode)
        RETURN cl
        LIMIT 1;
        """, Map.of("tokenId", tokenId, "repoName", repoName, "fileHash", fileHash)));
  }

  public Optional<Clazz> findClassByLandscapeTokenAndRepositoryAndFileHash(
      final String branchName, final String repoName, final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findClassByLandscapeTokenAndRepositoryAndFileHash(session, branchName, repoName,
        tokenId);
  }
}
