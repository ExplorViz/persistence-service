package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Function;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;

@ApplicationScoped
public class FunctionRepository {

  private static final String FIND_BY_FQN_AND_LANDSCAPE_TOKEN_STATEMENT = """
      MATCH (:Landscape {tokenId: $tokenId})
        -[:CONTAINS]->(:Trace)
        -[:CONTAINS]->(:Span)
        -[:REPRESENTS]->(func:Function {name: $name})
      MATCH (:Application {name: $appName})-[:HAS_ROOT]->(appRoot:Directory)
      MATCH p = (fqnRoot:Directory|FileRevision)-[:CONTAINS]->*(file:FileRevision)
      WHERE
        (appRoot)-[:CONTAINS]->(fqnRoot) AND
        (file)-[:CONTAINS]->(func) AND
        NOT (:Commit)-[:CONTAINS]->(file) AND
        all(j IN range(0, length(p)) WHERE nodes(p)[j].name = $pathSegments[j]) AND
        size(nodes(p)) = size($pathSegments)
      RETURN func;""";

  private static final String FIND_BY_FQN_AND_LANDSCAPE_TOKEN_AND_COMMIT_HASH_STATEMENT = """
      MATCH (l:Landscape {tokenId: $tokenId})
        -[:CONTAINS]->(:Repository)
        -[:HAS_ROOT]->(:Directory)
        -[:CONTAINS]->*(appRoot:Directory)
        <-[:HAS_ROOT]-(app:Application {name: $appName})
      MATCH p = (fqnRoot:Directory|FileRevision)
        -[:CONTAINS]->*(file:FileRevision)
        -[:CONTAINS]->(func:Function)
      WHERE
        (appRoot)-[:CONTAINS]->(fqnRoot) AND
        all(j IN range(0, length(p)) WHERE nodes(p)[j].name = $pathSegments[j]) AND
        size(nodes(p)) = size($pathSegments)
      MATCH (file)<-[:CONTAINS]-(:Commit {hash: $commitHash})
      RETURN func;""";

  public Optional<Function> findFunctionByApplicationNameAndFqnAndLandscapeToken(
      final Session session, final String applicationName, final String[] fqn,
      final String landscapeToken) {
    final String[] functionFilePath = Arrays.copyOfRange(fqn, 0, fqn.length - 1);
    final String functionName = fqn[fqn.length - 1];
    return Optional.ofNullable(
        session.queryForObject(Function.class, FIND_BY_FQN_AND_LANDSCAPE_TOKEN_STATEMENT,
            Map.of("tokenId", landscapeToken, "name", functionName, "pathSegments",
                functionFilePath, "appName", applicationName)));
  }

  public Optional<Function> findFunctionByApplicationNameAndFqnAndCommitHashAndLandscapeToken(
      final Session session, final String applicationName, final String[] fqn,
      final String commitHash, final String landscapeToken) {
    return Optional.ofNullable(session.queryForObject(Function.class,
        FIND_BY_FQN_AND_LANDSCAPE_TOKEN_AND_COMMIT_HASH_STATEMENT,
        Map.of("tokenId", landscapeToken, "pathSegments", fqn, "commitHash", commitHash, "appName",
            applicationName)));
  }

  /**
   * Retrieve all Functions from static analysis along with their fully-qualified names for a given
   * application at a particular commit.
   *
   * @return A map of each function's fqn to the corresponding Function object, separated by '/'.
   *     Note that since the fqn is derived from the node path, it may not be compliant to any
   *     standard notation (e.g. Java).
   */
  public Map<String, Function> findStaticFunctionsWithFqnForApplicationAndCommitAndLandscapeToken(
      final Session session, final String applicationName, final String commitHash,
      final String landscapeToken) {

    final Map<String, Function> filePathToFunctionMap = new HashMap<>();

    final Result result = session.query("""
            MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository)
              -[:HAS_ROOT]->(:Directory)
              -[:CONTAINS]->*(rd:Directory)<-[:HAS_ROOT]-(a:Application {name: $appName})
            MATCH p = (rd)-[:CONTAINS]->*(f:FileRevision)-[:CONTAINS]->(fn:Function)
            WHERE (:Commit {hash: $commitHash})-[:CONTAINS]->(f)
            RETURN DISTINCT
              fn AS function,
              reduce(fqn = nodes(p)[1].name, n IN nodes(p)[2..] | fqn + '/' + n.name) AS fqn;
            """,
        Map.of("tokenId", landscapeToken, "appName", applicationName, "commitHash", commitHash));

    result.queryResults().forEach(
        queryResult -> filePathToFunctionMap.put((String) queryResult.get("fqn"),
            (Function) queryResult.get("function")));

    return filePathToFunctionMap;
  }

  public Optional<Function> findFunction(final Session session, final String applicationName,
      final String[] fqn, final String landscapeToken) {
    return findFunctionByApplicationNameAndFqnAndLandscapeToken(session, applicationName, fqn,
        landscapeToken);
  }

  public Optional<Function> findFunction(final Session session, final String applicationName,
      final String[] fqn, final String commitHash, final String landscapeToken) {
    return findFunctionByApplicationNameAndFqnAndCommitHashAndLandscapeToken(session,
        applicationName, fqn, commitHash, landscapeToken);
  }
}
