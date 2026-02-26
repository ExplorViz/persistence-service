package net.explorviz.persistence.repository;

import com.google.common.collect.Lists;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.explorviz.persistence.ogm.Application;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class ApplicationRepository {

  private static final String FIND_BY_NAME_AND_LANDSCAPE_TOKEN = """
      MATCH (l:Landscape {tokenId: $tokenId})
        --*(appRoot:Directory)<-[h:HAS_ROOT]-(app:Application {name: $name})
      RETURN app, h, appRoot;
      """;

  // TODO does this fetch empty applications, i.e. applications with empty root dir?
  private static final String FIND_APPLICATIONS_WITH_FULL_TREE = """
      MATCH (a:Application)-[h:HAS_ROOT]->(rootDir:Directory)--*(:Landscape {tokenId: $tokenId})
      MATCH p = (a)-[h:HAS_ROOT]->(:Directory)-[:CONTAINS]->*(f:FileRevision)
      OPTIONAL MATCH (f)-[:CONTAINS]->(fn:Function)-->*(fnNode)
      UNWIND nodes(p) AS pathNode
      UNWIND relationships(p) AS rel
      RETURN DISTINCT a, h, pathNode, rel, fn, fnNode;
      """;

  private static final String FIND_STATIC_APPLICATION_NAMES_BY_LANDSCAPE_TOKEN = """
      MATCH (:Landscape {tokenId: $tokenId})
        -[:CONTAINS]->(:Repository)
        -[:HAS_ROOT]->(:Directory)
        -[:CONTAINS]->*(:Directory)<-[:HAS_ROOT]-(a:Application)
      RETURN DISTINCT a.name;
      """;

  @Inject
  private SessionFactory sessionFactory;

  public Optional<Application> findApplicationByNameAndLandscapeToken(final Session session,
      final String name, final String tokenId) {
    return Optional.ofNullable(
        session.queryForObject(Application.class, FIND_BY_NAME_AND_LANDSCAPE_TOKEN,
            Map.of("tokenId", tokenId, "name", name)));
  }

  public Optional<Application> findApplicationByNameAndLandscapeToken(final String name,
      final String tokenId) {
    final Session session = sessionFactory.openSession();
    return findApplicationByNameAndLandscapeToken(session, name, tokenId);
  }

  /**
   * Returns a list of fully hydrated Application objects with respect to the file structure,
   * meaning all files and corresponding directories are fetched. This includes fully hydrated
   * functions and classes. Empty if no Application is matched.
   */
  public List<Application> fetchAllFullyHydratedApplications(final Session session,
      final String landscapeToken) {
    return Lists.newArrayList(session.query(Application.class, FIND_APPLICATIONS_WITH_FULL_TREE,
        Map.of("tokenId", landscapeToken)));
  }

  /**
   * Return the names of all application nodes in a landscape which are contained in a repository,
   * i.e. the names of all applications which have static data available.
   */
  public List<String> findStaticApplicationNamesForLandscapeToken(final Session session,
      final String landscapeToken) {
    return Lists.newArrayList(
        session.query(String.class, FIND_STATIC_APPLICATION_NAMES_BY_LANDSCAPE_TOKEN,
            Map.of("tokenId", landscapeToken)));
  }

  public Application getOrCreateApplication(final Session session, final String name,
      final String tokenId) {
    return findApplicationByNameAndLandscapeToken(session, name, tokenId).orElse(
        new Application(name));
  }
}
