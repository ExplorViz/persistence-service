package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Function;
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

  public Function getOrCreateFunction(final Session session, final String fqn,
      final String tokenId) {
    final String[] splitFqn = fqn.split("\\.");
    return findFunctionByFqnAndLandscapeToken(session, fqn, tokenId).orElse(
        new Function(splitFqn[splitFqn.length - 1]));
  }

  public Optional<Function> findFunctionByLandscapeTokenAndRepositoryAndFilePath(
      final Session session, final String tokenId, final String repoName,
      final String[] pathSegments) {
    /*
    TODO: Create cypher query that tries to find a Function node restricted to a Landscape,
     a Repository, and a specific file path
     */
    return Optional.empty();
  }
}
