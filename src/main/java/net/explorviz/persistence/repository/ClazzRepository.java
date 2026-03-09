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
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseObjectForClearerAPI"})
public class ClazzRepository {

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Clazz> findClassDynamicData(final Session session, final String landscapeToken,
      final String applicationName, final String[] filePathSegments,
      final String classNameSegments) {
    return Optional.ofNullable(session.queryForObject(Clazz.class, """
        MATCH (:Landscape {tokenId: $tokenId})--*(app:Application {name: $appName})
        MATCH (app)-[:HAS_ROOT]->(appRoot:Directory)
        MATCH p = (pathRoot:Directory|FileRevision)-[:CONTAINS]->*(file:FileRevision)
        WHERE
          (appRoot)-[:CONTAINS]->(pathRoot) AND
          NOT (:Commit)-[:CONTAINS]->(file)) AND
          all(j IN range(0, length(p)) WHERE nodes(p)[j].name = $filePathSegments[j]) AND
          size(nodes(p)) = size($filePathSegments)
        MATCH p = (:Clazz)-[:CONTAINS]->*(class:Clazz)
        WHERE
          (file)-[:CONTAINS]->(class) AND
          all(j IN range(0, length(p)) WHERE nodes(p)[j].name = $classNameSegments[j]) AND
          size(nodes(p)) = size($classNameSegments)
        OPTIONAL MATCH (class)-[r]->(other)
        RETURN DISTINCT class, r, other;
        """, Map.of("tokenId", landscapeToken, "appName", applicationName, "filePathSegments",
        filePathSegments, "classNameSegments", classNameSegments)));
  }


  public Optional<Clazz> findClassByLandscapeTokenAndRepositoryAndFileHashAndClazzName(
      final Session session, final String tokenId, final String repoName, final String fileHash,
      final String clazzName) {
    return Optional.ofNullable(session.queryForObject(Clazz.class, """
        MATCH (:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(:Repository {name: $repoName})
          -[:CONTAINS]->(:Commit)
          -[:CONTAINS]->(f:FileRevision {hash: $fileHash})
          -[:CONTAINS]->(cl:Clazz {name: $clazzName})
        RETURN cl
        LIMIT 1;
        """, Map.of("tokenId", tokenId, "repoName", repoName, "fileHash", fileHash, "clazzName",
        clazzName)));
  }

  public Optional<Clazz> findClassByLandscapeTokenAndRepositoryAndClazzFqn(final Session session,
      final String tokenId, final String repoName, final String[] splitFqn, final String fileExt) {
    final String clazzName = splitFqn[splitFqn.length - 1];
    final String fileName = clazzName + fileExt;
    String[] fullFqn = new String[splitFqn.length + 1];
    System.arraycopy(splitFqn, 0, fullFqn, 0, splitFqn.length - 1);
    fullFqn[splitFqn.length - 1] = fileName;
    fullFqn[splitFqn.length] = clazzName;

    return Optional.ofNullable(session.queryForObject(Clazz.class, """
        MATCH (:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(r:Repository {name: $repoName})
          -[:CONTAINS]->(:Commit)
          -[:CONTAINS]->(file:FileRevision {name: $fileName})
          -[:CONTAINS]->(cl:Clazz {name: $clazzName})
        MATCH (r)-[:HAS_ROOT]->(root:Directory {name: $repoName})
        WITH root, $pathSegments AS pathSegments
        MATCH p = (root)-[:CONTAINS]->(:Directory)
                  -[:CONTAINS]->*(file)
                  -[:CONTAINS]->(cl)
        WHERE
        all(j IN range(1, length(p)-1) WHERE nodes(p)[j].name = pathSegments[j-1])
          AND size(nodes(p))-1 = size(pathSegments)
        RETURN cl;
        """, Map.of("tokenId", tokenId, "repoName", repoName, "clazzName", clazzName, "fileName",
        fileName, "pathSegments", fullFqn)));
  }

  public Optional<Clazz> findClassFromInheritingClass(final Session session, final String tokenId,
      final String repoName, final String clazzName) {
    return Optional.ofNullable(session.queryForObject(Clazz.class, """
        MATCH (:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(:Repository {name: $repoName})
          -[:CONTAINS]->(:Commit)
          -[:CONTAINS]->(:FileRevision)
          -[:CONTAINS]->(:Clazz)
          -[:INHERITS]->(cl:Clazz {name: $clazzName})
        RETURN cl
        LIMIT 1;
        """, Map.of("tokenId", tokenId, "repoName", repoName, "clazzName", clazzName)));
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
