package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.proto.FileData;
import org.jboss.logging.Logger;
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

  private static final Logger LOGGER = Logger.getLogger(FileRevisionRepository.class);


  private FileRevision createFilePath(final Session session, final Directory startingDirectory,
      final String[] remainingPath) {
    System.out.println("createFilePath called");
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

  private Map<String, Object> findLongestPathMatch(final Session session, final String[] filePath,
      final String applicationName) {
    final Result result = session.query(FIND_LONGEST_PATH_MATCH,
        Map.of("pathSegments", filePath, "applicationName", applicationName));

    final Iterator<Map<String, Object>> resultIterator = result.queryResults().iterator();
    if (!resultIterator.hasNext()) {
      throw new NoSuchElementException("resultIterator empty");
    }

    return resultIterator.next();
  }

  private FileRevision createFileStructure(final Session session, final String[] filePath,
      final String applicationName) {
    System.out.println("createFileStructure called");

    final Map<String, Object> resultMap = findLongestPathMatch(session, filePath, applicationName);

    final String[] remainingPath =
        resultMap.get("remainingPath") instanceof String[] p ? p : new String[0];
    if (remainingPath.length == 0) {
      System.out.println("Already existing");
      return resultMap.get("existingNode") instanceof FileRevision fileRev ? fileRev : null;
    }

    System.out.println("3");

    final Directory startingDirectory =
        resultMap.get("existingNode") instanceof Directory dir ? dir : null;
    if (startingDirectory == null) {
      // Root directory not matched, application does not exist or has no root
      throw new NoSuchElementException("startingDirectory is null");
    }

    System.out.println("4");

    return createFilePath(session, startingDirectory, remainingPath);
  }

  public void createFileStructureFromFunction(final Session session, final Function function,
      final String functionFqn, final Application application, final Landscape landscape,
      final String commitId) {
    final String[] splitFqn = functionFqn.split("\\.");
    final String[] pathSegments = Arrays.copyOfRange(splitFqn, 0, splitFqn.length - 1);
    if (pathSegments.length < 1) {
      return;
    }

    FileRevision file = createFileStructure(session, pathSegments, application.getName());
    if (file == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("FileRevision exists but doesn't exist!?!");
      }
      return;
    }

    file.addFunction(function);
    session.save(file);

    if (commitId != null) {
      Commit commit = commitRepository.getOrCreateCommit(session, commitId, landscape.getTokenId());
      commit.addFileRevision(file);
      session.save(commit);
    }
  }

  public void createFileStructureFromFileData(final Session session, final FileData fileData) {

    //    System.out.println(fileData.getPackageName());
    //    System.out.println(fileData.getCommitID());
    //    System.out.println(fileData.getLandscapeToken());
    //    System.out.println(fileData.getApplicationName());
    //    System.out.println("");

    final Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, fileData.getLandscapeToken());

    final Application application =
        applicationRepository.getOrCreateApplication(session, fileData.getApplicationName(),
            fileData.getLandscapeToken());

    if (application.getRootDirectory() == null) {
      final Directory applicationRoot = new Directory("*");
      application.setRootDirectory(applicationRoot);
    }
    landscape.addApplication(application);

    /* TODO: What if dynamic data already created a FileRevision with the function but (perhaps)
        TODO: from a different commit?
     If dynamic data created FileRevision without Commit -> ignore and create new FileRevision
      -> Checken, ob Datei ohne Commit existiert
        *.a.b.c.file
        x.y.a.b.c.file
      -> Copy von Datei erstellen

     If FileRevision with name does not exist -> create new FileRevision
     */
    final FileRevision file = createFileStructure(session, fileData.getPackageName().split("\\."),
        fileData.getApplicationName());

    final Commit commit = commitRepository.getOrCreateCommit(session, fileData.getCommitID(),
        fileData.getLandscapeToken());
    commit.addFileRevision(file);

    session.save(commit);
  }

  public FileRevision createFileStructureFromFilePath(final Session session, final String filePath,
      final Application application, final Landscape landscape) {
    System.out.println("createFileStructureFromFilePath called");

    final Map<String, Object> resultMap =
        findLongestPathMatch(session, filePath.split("/"), application.getName());

    final String[] remainingPath =
        resultMap.get("remainingPath") instanceof String[] p ? p : new String[0];
    if (remainingPath.length == 0) {
      System.out.println("Already existing");
      FileRevision existingFile =
          resultMap.get("existingNode") instanceof FileRevision fileRev ? fileRev : null;
//      Directory dir = session.queryForObject(Directory.class,
//          "MATCH (d:Directory)-[:CONTAINS]->(f:FileRevision) WHERE elementId(f)=$fileId RETURN d;",
//          Map.of("fileId", existingFile.getId()));

      FileRevision copiedFile =
          new FileRevision(existingFile.getName(), existingFile.getFunctions());

//      dir.addFileRevision(copiedFile);
//      session.save(dir);

      return copiedFile;
    }

    FileRevision file = createFileStructure(session, filePath.split("/"), application.getName());

    System.out.println(file);

    return file;
  }
}
