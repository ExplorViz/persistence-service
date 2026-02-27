package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Function;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class FunctionRepository {

  private static final String FIND_BY_FQN_AND_LANDSCAPE_TOKEN_STATEMENT = """
      MATCH (:Landscape {tokenId: $tokenId})-[:CONTAINS]->(:Trace)-[:CONTAINS]->
          (:Span)-[:REPRESENTS]->(f:Function {name: $name})
      WITH f, $pathSegments AS pathSegments
      MATCH p = (:Directory|FileRevision)-[:CONTAINS]->*(:Directory|FileRevision)-[:CONTAINS]->(f)
      WHERE
      all(j IN range(0, length(p)-1) WHERE nodes(p)[j].name = pathSegments[j])
      RETURN f;""";

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Function> findFunctionByFqnAndLandscapeToken(final Session session,
      final String fqn, final String tokenId) {
    final String[] splitFqn = fqn.split("\\.");
    final String[] functionPath = Arrays.copyOfRange(splitFqn, 0, splitFqn.length - 1);
    final String functionName = splitFqn[splitFqn.length - 1];
    return Optional.ofNullable(
        session.queryForObject(Function.class, FIND_BY_FQN_AND_LANDSCAPE_TOKEN_STATEMENT,
            Map.of("tokenId", tokenId, "name", functionName, "pathSegments", functionPath)));
  }

  public Optional<Function> findFunctionByFqnAndLandscapeToken(final String tokenId,
      final String fqn) {
    final Session session = sessionFactory.openSession();
    return findFunctionByFqnAndLandscapeToken(session, fqn, tokenId);
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

    result.queryResults()
        .forEach(queryResult -> filePathToFunctionMap.put((String) queryResult.get("fqn"),
            (Function) queryResult.get("function")));

    return filePathToFunctionMap;
  }

  public Function getOrCreateFunction(final Session session, final String fqn,
      final String tokenId) {
    final String[] splitFqn = fqn.split("\\.");
    return findFunctionByFqnAndLandscapeToken(session, fqn, tokenId).orElse(
        new Function(splitFqn[splitFqn.length - 1]));
  }
}
