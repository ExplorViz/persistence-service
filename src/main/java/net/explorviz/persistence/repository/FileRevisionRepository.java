package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
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

  @Inject
  private ApplicationRepository applicationRepository;

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private CommitRepository commitRepository;

  private FileRevision createFilePath(final Session session, final Directory startingDirectory,
      final String[] remainingPath) {
    Directory currentDirectory = startingDirectory;
    for (int i = 0; i < remainingPath.length - 1; i++) {
      final Directory newDirectory = new Directory(remainingPath[i]);
      currentDirectory.addSubdirectory(newDirectory);
      currentDirectory = newDirectory;
    }

    final FileRevision file = new FileRevision(remainingPath[remainingPath.length - 1]);
    currentDirectory.addFileRevision(file);
    session.save(startingDirectory);
    return file;
  }

  private FileRevision createFileStructure(final Session session,
      final String[] filePath, final String applicationName) {
    final Result result = session.query(FIND_LONGEST_PATH_MATCH,
        Map.of("pathSegments", filePath, "applicationName", applicationName));

    final Iterator<Map<String, Object>> resultIterator = result.queryResults().iterator();
    if (!resultIterator.hasNext()) {
      throw new NoSuchElementException("resultIterator empty");
    }

    final Map<String, Object> resultMap = resultIterator.next();

    String[] remainingPath =
        resultMap.get("remainingPath") instanceof String[] p ? p : new String[0];
    if (remainingPath.length == 0) {
      return null;
    }

    final Directory startingDirectory =
        resultMap.get("existingNode") instanceof Directory dir ? dir : null;
    if (startingDirectory == null) {
      // Root directory not matched, application does not exist or has no root
      throw new NoSuchElementException("startingDirectory is null");
    }

    return createFilePath(session, startingDirectory, remainingPath);
  }

  public void createFileStructureFromFunction(final Session session, final Function function,
      final Application application) {
    final String[] splitFqn = function.getFqn().split("\\.");
    final String[] pathSegments = Arrays.copyOfRange(splitFqn, 0, splitFqn.length - 1);
    if (pathSegments.length < 1) {
      return;
    }

    final FileRevision file =
        createFileStructure(session, pathSegments, application.getName());
    if (file == null) {
      return;
    }

    file.addFunction(function);
    session.save(file);
  }

  public void createFileStructureFromFileData(final Session session, final String fileName,
      final String packageName, final String commitId, final String landscapeToken,
      final String applicationName) {

    //    System.out.println(fileName);
    //    System.out.println(packageName);
    //    System.out.println(commitId);
    //    System.out.println(landscapeToken);
    //    System.out.println(applicationName);
    //    System.out.println("");

    final Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, landscapeToken);

    final Application application =
        applicationRepository.getOrCreateApplication(session, applicationName,
            landscapeToken);

    if (application.getRootDirectory() == null) {
      final Directory applicationRoot = new Directory("*");
      application.setRootDirectory(applicationRoot);
    }
    landscape.addApplication(application);

    // TODO: What if dynamic data already created a FileRevision with the function but (perhaps) from a different commit?
    final FileRevision file =
        createFileStructure(session, packageName.split("\\."), applicationName);

    final Commit commit = commitRepository.getOrCreateCommit(session, commitId, landscapeToken);
    commit.addFileRevision(file);

    session.save(commit);
  }
}
