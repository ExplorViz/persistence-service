package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
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
      OPTIONAL MATCH (:Application {name: applicationName})-[:HAS_ROOT]->(rootDir:Directory)
      OPTIONAL MATCH p = (fqnRoot:Directory|FileRevision)-[:CONTAINS]->*(:Directory|FileRevision)
      WHERE
        (rootDir)-[:CONTAINS]->(fqnRoot) AND
        all(j IN range(0, length(p)) WHERE nodes(p)[j].name = pathSegments[j]) AND
        (length(p) + 1 < size(pathSegments) XOR "FileRevision" IN labels(last(nodes(p))))
      RETURN
        coalesce(last(nodes(p)), rootDir) AS existingNode,
        pathSegments[coalesce(length(p)+1, 0)..] AS remainingPath
      ORDER BY size(nodes(p)) DESC
      LIMIT 1;""";

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

    final Result result = session.query(FIND_LONGEST_PATH_MATCH,
        Map.of("pathSegments", pathSegments, "applicationName", application.getName()));

    final Iterator<Map<String, Object>> resultIterator = result.queryResults().iterator();
    if (!resultIterator.hasNext()) {
      throw new NoSuchElementException();
    }

    final Map<String, Object> resultMap = resultIterator.next();

    String[] remainingPath = (String[]) resultMap.get("remainingPath");
    if (remainingPath == null) {
      remainingPath = pathSegments;
    } else if (remainingPath.length == 0) {
      return;
    }

    final Directory startingDirectory =
        resultMap.get("existingNode") instanceof Directory dir ? dir : null;
    if (startingDirectory == null) {
      // Root directory not matched, application does not exist or has no root
      throw new NoSuchElementException();
    }

    final FileRevision file = createFileStructure(startingDirectory, remainingPath);
    file.addFunction(function);

    session.save(startingDirectory);
  }

  private FileRevision createFileStructure(final Directory startingDirectory,
      final String[] remainingPath) {
    Directory currentDirectory = startingDirectory;
    for (int i = 0; i < remainingPath.length - 1; i++) {
      final Directory newDirectory = new Directory(remainingPath[i]);
      currentDirectory.addSubdirectory(newDirectory);
      currentDirectory = newDirectory;
    }

    final FileRevision file = new FileRevision(remainingPath[remainingPath.length - 1]);
    currentDirectory.addFileRevision(file);
    return file;
  }
}
