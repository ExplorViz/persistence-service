package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Clazz;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class ClazzRepository {

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Clazz> findClassByLandscapeTokenAndRepositoryAndFileHash(final Session session,
      final String tokenId, final String repoName, final String fileHash) {
    return Optional.ofNullable(session.queryForObject(Clazz.class, """
        MATCH (:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(:Repository {name: $repoName})
          -[:CONTAINS]->(:Commit)
          -[:CONTAINS]->(f:FileRevision {hash: $fileHash})
          -[:CONTAINS]->(cl:Clazz)
        RETURN cl
        LIMIT 1;
        """, Map.of("tokenId", tokenId, "repoName", repoName, "fileHash", fileHash)));
  }

  public Optional<Clazz> findClassByLandscapeTokenAndRepositoryAndFileHash(final String branchName,
      final String repoName, final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findClassByLandscapeTokenAndRepositoryAndFileHash(session, branchName, repoName,
        tokenId);
  }

  /**
   * Retrieve all classes from static analysis along with their fully-qualified names for a given
   * application at a particular commit.
   *
   * @return A map of each class's fqn to the corresponding Clazz object, separated by '/'. To
   *     account for inner classes, the filename is followed by the class name. Note that since the
   *     fqn is derived from the node path, it may not be compliant to any standard notation (e.g.
   *     Java).
   */
  public Map<String, Clazz> findStaticClassesWithFqnForApplicationAndCommitAndLandscapeToken(
      final Session session, final String applicationName, final String commitHash,
      final String landscapeToken) {

    final Map<String, Clazz> filePathToClazzMap = new HashMap<>();

    final Result result = session.query("""
            MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository)
              -[:HAS_ROOT]->(:Directory)
              -[:CONTAINS]->*(rd:Directory)<-[:HAS_ROOT]-(a:Application {name: $appName})
            MATCH p = (rd)-[:CONTAINS]->*(f:FileRevision)-[:CONTAINS]->(c:Clazz)
            WHERE (:Commit {hash: $commitHash})-[:CONTAINS]->(f)
            RETURN DISTINCT
              c AS clazz,
              reduce(fqn = nodes(p)[1].name, n IN nodes(p)[2..] | fqn + '/' + n.name) AS fqn;
            """,
        Map.of("tokenId", landscapeToken, "appName", applicationName, "commitHash", commitHash));

    result.queryResults().forEach(
        queryResult -> filePathToClazzMap.put((String) queryResult.get("fqn"),
            (Clazz) queryResult.get("clazz")));

    return filePathToClazzMap;
  }
}
