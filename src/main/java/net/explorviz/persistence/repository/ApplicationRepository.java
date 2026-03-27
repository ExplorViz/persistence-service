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
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseObjectForClearerAPI"})
public class ApplicationRepository {

  @Inject private SessionFactory sessionFactory;

  public Optional<Application> findApplicationByNameAndLandscapeToken(
      final Session session, final String name, final String tokenId) {
    return Optional.ofNullable(
        session.queryForObject(
            Application.class,
            """
        MATCH (l:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(app:Application {name: $name})
          -[h:HAS_ROOT]->(appRoot:Directory)
        RETURN app, h, appRoot;
        """,
            Map.of("tokenId", tokenId, "name", name)));
  }

  /**
   * Returns a list of fully hydrated Application objects with respect to the file structure,
   * meaning all files and corresponding directories are fetched. This includes fully hydrated
   * functions and classes. Empty if no Application is matched.
   */
  public List<Application> fetchAllFullyHydratedApplications(final Session session,
      final String landscapeToken) {
    return Lists.newArrayList(session.query(Application.class, """
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(a:Application)
        CALL apoc.path.subgraphAll(a, {
          relationshipFilter: "HAS_ROOT>|CONTAINS>"
        })
        YIELD relationships
        UNWIND relationships as r
        RETURN startNode(r), r, endNode(r);
        """,
            Map.of("tokenId", landscapeToken)));
  }

  /**
   * Fetch the hydrated Application objects containing the files of two particular commits. The
   * commits are hydrated with respect to the file structure of the specified commits, including
   * file contents such as functions and classes. The commits are combined in the sense that any
   * file which is included in either commit is returned, i.e. the union of the commit's file
   * structures is fetched.
   *
   * @param session OGM session object
   * @param firstCommitHash Hash of the first commit whose files to include
   * @param secondCommitHash Hash of the second commit whose files to include
   * @param landscapeToken Identifier of the software landscape
   * @return A list of the Application objects belonging to the specified commits, hydrated to
   *     include all files and file contents in the given commits that are part of the application.
   *     Empty if no application is matched.
   */
  public List<Application> fetchApplicationsHydratedForTwoCommits(
      final Session session,
      final String landscapeToken,
      final String firstCommitHash,
      final String secondCommitHash) {
    return Lists.newArrayList(
        session.query(
            Application.class,
            """
            MATCH (:Landscape {tokenId: $tokenId})
              -[:CONTAINS]->(:Repository)
              -[:CONTAINS]->(c:Commit WHERE c.hash = $firstCommitHash OR c.hash = $secondCommitHash)
              -[:CONTAINS]->(f:FileRevision)

            CALL apoc.path.subgraphAll(f, {
              relationshipFilter: "<HAS_ROOT|<CONTAINS",
              labelFilter: "+Directory|+Application"
            })
            YIELD relationships AS filePathRelations

            CALL apoc.path.subgraphAll(f, {
              relationshipFilter: "CONTAINS>"
            })
            YIELD relationships AS fileContentRelations

            UNWIND filePathRelations + fileContentRelations as r
            RETURN startNode(r), r, endNode(r);
            """,
            Map.of(
                "tokenId",
                landscapeToken,
                "firstCommitHash",
                firstCommitHash,
                "secondCommitHash",
                secondCommitHash)));
  }

  /**
   * Return the names of all application nodes in a landscape which are contained in a repository,
   * i.e. the names of all applications which have static data available.
   */
  public List<String> findStaticApplicationNamesForLandscapeToken(final Session session,
      final String landscapeToken) {
    return Lists.newArrayList(session.query(String.class, """
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(a:Application)
        WHERE (l)
          -[:CONTAINS]->(:Repository)
          -[:HAS_ROOT]->(:Directory)
          -[:CONTAINS*]->(:Directory)<-[:HAS_ROOT]-(a)
        RETURN DISTINCT a.name;
        """,
            Map.of("tokenId", landscapeToken)));
  }
}
