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

  @Inject SessionFactory sessionFactory;

  public Optional<Application> findApplicationByNameAndLandscapeToken(
      final Session session, final String name, final String tokenId) {
    return Optional.ofNullable(
        session.queryForObject(
            Application.class,
            """
        MATCH (l:Landscape {tokenId: $tokenId})
        MATCH (app:Application {name: $name})-[h:HAS_ROOT]->(appRoot:Directory)
        WHERE
          (appRoot)-[*]-(l)
        RETURN app, h, appRoot;
        """,
            Map.of("tokenId", tokenId, "name", name)));
  }

  /**
   * Returns a list of Application objects hydrated with respect to the file structure generated
   * from runtime analysis, meaning all files and corresponding directories gathered from trace
   * analysis are fetched. This includes fully hydrated functions and classes. A file is considered
   * to be gathered from runtime analysis if it contains a function that is represented by at least
   * one span. Empty if no Application is matched.
   */
  // TODO this should only include functions that have a span attached, otherwise we also include
  // information not actually gathered from runtime analysis
  public List<Application> fetchAllApplicationsHydratedForRuntimeData(
      final Session session, final String landscapeToken) {
    return Lists.newArrayList(
        session.query(
            Application.class,
            """
        MATCH (l:Landscape {tokenId: $tokenId})
        MATCH (func:Function)
        WHERE
          (l)-[:CONTAINS]->(:Trace)-[:CONTAINS]->(:Span)-[:REPRESENTS]->(func)

        MATCH p = (:Application)-[:CONTAINS]->*(file:FileRevision)
        WHERE (file)-[:CONTAINS]->(func)

        CALL apoc.path.subgraphAll(file, {
          relationshipFilter: "CONTAINS>"
        })
        YIELD relationships AS fileContentRels

        WITH relationships(p) + fileContentRels AS rels
        UNWIND rels as r
        RETURN startNode(r), r, endNode(r);
        """,
            Map.of("tokenId", landscapeToken)));
  }

  /**
   * Returns a list of all Application objects that are contained in the given repository. The
   * application objects are hydrated with respect to the file structure of a specific commit,
   * meaning all files and corresponding directories within the commit are also fetched. This
   * includes fully hydrated functions and classes. Empty if no Application is matched.
   */
  public List<Application> fetchHydratedApplicationsInRepositoryForCommit(
      final Session session,
      final String landscapeToken,
      final String repositoryName,
      final String commitHash) {
    return Lists.newArrayList(
        session.query(
            Application.class,
            """
        MATCH (l:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(:Repository {name: $repoName})
          -[:CONTAINS]->(c:Commit {hash: $commitHash})
        MATCH (c)-[:CONTAINS]->(f:FileRevision)

        CALL apoc.path.subgraphAll(f, {
          relationshipFilter: "CONTAINS>"
        })
        YIELD relationships AS fileContentRels

        MATCH p = (:Application)-[:HAS_ROOT]->(:Directory)-[:CONTAINS]->*(f:FileRevision)

        WITH relationships(p) + fileContentRels AS rels
        UNWIND rels as r
        RETURN DISTINCT startNode(r), r, endNode(r);
        """,
            Map.of(
                "tokenId", landscapeToken, "repoName", repositoryName, "commitHash", commitHash)));
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
   * @param landscapeToken String identifier of the visualization landscape
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

            MATCH (f:FileRevision)
            WHERE (c)-[:CONTAINS]->(f)

            CALL apoc.path.subgraphAll(f, {
              relationshipFilter: "CONTAINS>"
            })
            YIELD relationships AS fileContentRelations

            MATCH p = (:Application)-[:CONTAINS]->*(f:FileRevision)

            WITH relationships(p) + fileContentRelations AS rels
            UNWIND rels as r
            RETURN DISTINCT startNode(r), r, endNode(r);
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
  public List<String> findStaticApplicationNamesForLandscapeToken(
      final Session session, final String landscapeToken) {
    return Lists.newArrayList(
        session.query(
            String.class,
            """
        MATCH (:Landscape {tokenId: $tokenId})
          -[:CONTAINS]->(:Repository)
          -[:HAS_ROOT]->(:Directory)
          -[:CONTAINS]->*(:Directory)<-[:HAS_ROOT]-(a:Application)
        RETURN DISTINCT a.name;
        """,
            Map.of("tokenId", landscapeToken)));
  }

  public Application getOrCreateApplication(
      final Session session, final String name, final String tokenId) {
    return findApplicationByNameAndLandscapeToken(session, name, tokenId)
        .orElse(new Application(name));
  }
}
