package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class FileRevisionRepository {
  private static final String FIND_LONGEST_PATH_MATCH = """
      WITH $pathSegments AS pathSegments, $applicationName as applicationName
      OPTIONAL CALL (pathSegments, applicationName) {
          UNWIND range(size(pathSegments)-1, 0, -1) AS i
          MATCH p = (fqnRoot:Directory|FileRevision)
                    ((:Directory)-[:CONTAINS]->(:Directory))*
                    ((:Directory)-[:CONTAINS]->(:FileRevision)){0,1}
          WHERE
              (:Application {name: applicationName})-[:HAS_ROOT]->(:Directory)
                  -[:CONTAINS]->(fqnRoot) AND
              length(p) = i AND
              all(j IN range(0, i) WHERE nodes(p)[j].name = pathSegments[j])
          RETURN p
          LIMIT 1
      }
      RETURN nodes(p)[-1] AS existingNode, pathSegments[length(p)+1..] AS remainingPath;""";

  @Inject
  private SessionFactory sessionFactory;

  // TODO alles dynamisch
  // TODO verhalten fehlt wenn statische daten schon vorhanden
  public void createFileStructureFromFunction(final Session session, final Function function,
      final Application application) {
    final String[] splitFqn = function.getFqn().split("\\.");
    final String[] pathSegments = Arrays.copyOfRange(splitFqn, 0, splitFqn.length - 1);
    if (pathSegments.length < 1) {
      return;
    }

    final Result result =
        session.query(FIND_LONGEST_PATH_MATCH,
            Map.of("pathSegments", pathSegments, "applicationName", application.getName()));

    final Map<String, ?> resultObj = result.queryResults().iterator().next();

    if (resultObj.get("existingNode") instanceof FileRevision) {
      return;
    }

    Directory startingDirectory = (Directory) resultObj.get("existingNode");
    if (startingDirectory == null) {
      final Directory applicationRoot = application.getRootDirectory();
      if (pathSegments.length == 1) {
        applicationRoot.addFileRevision(new FileRevision(pathSegments[0]));
        session.save(applicationRoot);
        return;
      }
      startingDirectory = new Directory(pathSegments[0]);
      applicationRoot.addSubdirectory(startingDirectory);
      session.save(applicationRoot);
    }

    String[] remainingPath = (String[]) resultObj.get("remainingPath");
    if (remainingPath == null) {
      remainingPath = Arrays.copyOfRange(pathSegments, 1, pathSegments.length);
    }

    Directory currentDirectory = startingDirectory;
    for (int i = 0; i < remainingPath.length - 1; i++) {
      final Directory newDirectory = new Directory(remainingPath[i]);
      currentDirectory.addSubdirectory(newDirectory);
      session.save(currentDirectory);
      currentDirectory = newDirectory;
    }

    final FileRevision file = new FileRevision(pathSegments[pathSegments.length - 1]);
    file.addFunction(function);
    currentDirectory.addFileRevision(file);
    session.save(List.of(currentDirectory, file));
  }
}
