package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.proto.FileIdentifier;
import org.jboss.logging.Logger;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class FileRevisionRepository {
  /* TODO: Nicht sicher, ob die hier nach der Modelländerung noch 100% funktioniert
      - Mir fehlt jegliche Zuordnung zu Landscape
   */
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
  private DirectoryRepository directoryRepository;

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private CommitRepository commitRepository;

  private static final Logger LOGGER = Logger.getLogger(FileRevisionRepository.class);


  /* TODO: Think about to change how the dynamic data creates the file structure.
      Maybe splitting the query into multiple sub-routines as in createFileStructureFromStaticData
   */

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
    final Map<String, Object> resultMap = findLongestPathMatch(session, filePath, applicationName);

    final String[] remainingPath =
        resultMap.get("remainingPath") instanceof String[] p ? p : new String[0];
    if (remainingPath.length == 0) {
      return resultMap.get("existingNode") instanceof FileRevision fileRev ? fileRev : null;
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
      final String functionFqn, final Application application, final Landscape landscape,
      final String commitId) {
    final String[] splitFqn = functionFqn.split("\\.");
    final String[] pathSegments = Arrays.copyOfRange(splitFqn, 0, splitFqn.length - 1);
    if (pathSegments.length < 1) {
      return;
    }

    final FileRevision file = createFileStructure(session, pathSegments, application.getName());
    if (file == null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("FileRevision exists but doesn't exist!?!");
      }
      return;
    }

    file.addFunction(function);
    session.save(file);

    if (commitId != null) {
      final Commit commit =
          commitRepository.getOrCreateCommit(session, commitId, landscape.getTokenId());
      commit.addFileRevision(file);
      session.save(commit);
    }
  }

  public FileRevision createFileStructureFromStaticData(final Session session,
      final FileIdentifier fileIdentifier, final String repoName, final String landscapeTokenId) {
    final String[] pathSegments = fileIdentifier.getFilePath().split("/");
    final String[] directorySegments = Arrays.copyOfRange(pathSegments, 0, pathSegments.length - 2);

    final FileRevision file =
        new FileRevision(fileIdentifier.getFileHash(), pathSegments[pathSegments.length - 1]);
    /* TODO: Maybe add repoName as first element of directorySegments
             (depending on how the paths are built)
     */
    final Directory parentDir =
        directoryRepository.createDirectoryStructureAndReturnLastDirStaticData(session,
            directorySegments, repoName, landscapeTokenId);
    parentDir.addFileRevision(file);

    session.save(List.of(parentDir, file));

    return file;
  }

  public Optional<FileRevision> getFileRevisionFromHash(final Session session,
      final String fileHash, final String repoName, final String landscapeTokenId) {
    return Optional.ofNullable(session.queryForObject(FileRevision.class, """
        MATCH (l:Landscape {tokenId: $tokenId})-[:CONTAINS]->(r:Repository {name: $repoName})
        MATCH (r)-[:CONTAINS]->(c:Commit)-[:CONTAINS]->(f:FileRevision {hash: $fileHash})
        RETURN f
        LIMIT 1;
        """, Map.of("tokenId", landscapeTokenId, "repoName", repoName, "fileHash", fileHash)));
  }
}
