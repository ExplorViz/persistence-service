package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import net.explorviz.persistence.ogm.Function;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class FunctionRepository {

  private static final String FIND_BY_FQN_AND_LANDSCAPE_TOKEN_STATEMENT = """
      MATCH (:Landscape {tokenId: $tokenId})-[:CONTAINS]->(:Trace)-[:CONTAINS]->
            (:Span)-[:REPRESENTS]->(f:Function {fqn: $fqn})
      RETURN f;
      """;

  @Inject
  private SessionFactory sessionFactory;

  public Function findFunctionByFqnAndLandscapeToken(final Session session,
                                                     final String functionFqn,
                                                     final String tokenId) {
    return session.queryForObject(Function.class, FIND_BY_FQN_AND_LANDSCAPE_TOKEN_STATEMENT,
        Map.of("tokenId", tokenId, "fqn", functionFqn));
  }

  public Function findFunctionByFqnAndLandscapeToken(final String tokenId,
                                                     final String functionFqn) {
    final Session session = sessionFactory.openSession();
    return findFunctionByFqnAndLandscapeToken(session, tokenId, functionFqn);
  }
}
