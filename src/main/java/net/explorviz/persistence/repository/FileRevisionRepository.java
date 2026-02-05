package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.proto.FileIdentifier;
import org.jboss.logging.Logger;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class FileRevisionRepository {

  private static final String FIND_LONGEST_PATH_MATCH_FOR_FQN_WITHOUT_COMMIT = """
      MATCH (:Landscape {tokenId: $tokenId})--*(appRootDir:Directory)
            <-[:HAS_ROOT]-(app:Application {name: $appName})
      OPTIONAL MATCH p = (fqnRoot:Directory|FileRevision)
            -[:CONTAINS]->*(lastNode:Directory|FileRevision)
      WHERE
        (appRootDir)-[:CONTAINS]->(fqnRoot) AND
        all(j IN range(0, length(p)) WHERE nodes(p)[j].name = $pathSegments[j]) AND
        (length(p) + 1 < size($pathSegments) XOR "FileRevision" IN labels(lastNode)) AND
        NOT EXISTS {
            (:Commit)-[:CONTAINS]->(lastNode)
          }
      RETURN
        coalesce(lastNode, appRootDir) AS existingNode,
        $pathSegments[coalesce(length(p)+1, 0)..] AS remainingPath
      ORDER BY length(p) DESC
      LIMIT 1;""";

  private static final String FIND_LONGEST_PATH_MATCH_FOR_FQN_WITH_COMMIT = """
      MATCH (:Landscape {tokenId: $tokenId})--*(appRootDir:Directory)
            <-[:HAS_ROOT]-(app:Application {name: $appName})
      OPTIONAL MATCH p = (fqnRoot:Directory|FileRevision)
            -[:CONTAINS]->*(lastNode:Directory|FileRevision)
      WHERE
        (appRootDir)-[:CONTAINS]->(fqnRoot) AND
        all(j IN range(0, length(p)) WHERE nodes(p)[j].name = $pathSegments[j]) AND
        (length(p) + 1 < size($pathSegments) XOR ("FileRevision" IN labels(lastNode) AND
          EXISTS {
            (:Commit {hash: $commitHash})-[:CONTAINS]->(lastNode)
          })
      RETURN
        coalesce(lastNode, appRootDir) AS existingNode,
        $pathSegments[coalesce(length(p)+1, 0)..] AS remainingPath
      ORDER BY length(p) DESC
      LIMIT 1;""";

  @Inject
  private SessionFactory sessionFactory;

  @Inject
  private ApplicationRepository applicationRepository;

  @Inject
  private DirectoryRepository directoryRepository;

  @Inject
  private LandscapeRepository landscapeRepository;

  private static final Logger LOGGER = Logger.getLogger(FileRevisionRepository.class);

  private FileRevision createRemainingFilePath(final Session session,
      final Directory startingDirectory,
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

  private Map<String, Object> findLongestPathMatchForFqn(final Session session,
      final String[] fileFqn, final String applicationName, final String landscapeToken,
      @Nullable final String commitHash) {

    Result result;

    if (commitHash != null) {
      result = session.query(FIND_LONGEST_PATH_MATCH_FOR_FQN_WITH_COMMIT,
          Map.of("pathSegments", fileFqn, "appName", applicationName, "tokenId", landscapeToken,
              "commitHash", commitHash));
    } else {
      result = session.query(FIND_LONGEST_PATH_MATCH_FOR_FQN_WITHOUT_COMMIT,
          Map.of("pathSegments", fileFqn, "appName", applicationName, "tokenId", landscapeToken));
    }

    final Iterator<Map<String, Object>> resultIterator = result.queryResults().iterator();
    if (!resultIterator.hasNext()) {
      throw new NoSuchElementException("resultIterator empty");
    }

    return resultIterator.next();
  }

  /**
   * Create any missing Directory / FileRevision nodes according to the provided FQN for an existing
   * Application object which is already connected to the Landscape graph.
   *
   * @param session         OGM session object.
   * @param applicationName Name of an existing application which is already connected to the
   *                        Landscape with the given token.
   * @param splitFileFqn    File FQN starting from application root (not inclusive), e.g. ["net",
   *                        "explorviz", "persistence", "MyClass.java"]
   * @return The existing or newly created FileRevision according to the provided FQN
   */
  public FileRevision createFileStructureForExistingApplicationFromFileFqn(final Session session,
      final String[] splitFileFqn, final String applicationName, final String landscapeToken,
      @Nullable final String commitHash) {

    if (splitFileFqn.length < 1) {
      throw new IllegalArgumentException("FQN must not be empty");
    }

    final Map<String, Object> resultMap =
        findLongestPathMatchForFqn(session, splitFileFqn, applicationName, landscapeToken,
            commitHash);

    final String[] remainingPath =
        resultMap.get("remainingPath") instanceof String[] p ? p : new String[0];
    if (remainingPath.length == 0) {
      if (resultMap.get("existingNode") instanceof FileRevision fileRev) {
        return fileRev;
      }
      throw new NoSuchElementException("remainingPath is length 0, but result is not FileRevision");
    }

    final Directory startingDirectory =
        resultMap.get("existingNode") instanceof Directory dir ? dir : null;
    if (startingDirectory == null) {
      // Root directory not matched, application does not exist or has no root
      throw new NoSuchElementException("startingDirectory is null. Does the application exist?");
    }

    return createRemainingFilePath(session, startingDirectory, remainingPath);
  }

  /**
   * Create Directory / FileRevision nodes according to the provided FQN for a newly created
   * Application object which is not yet connected to the Landscape graph.
   *
   * @param session      OGM session object.
   * @param application  Newly created application object, assumed not to have a root directory.
   * @param splitFileFqn File FQN starting from application root (not inclusive), e.g. ["net",
   *                     "explorviz", "persistence", "MyClass.java"]
   * @return The newly created FileRevision according to the provided FQN
   */
  public FileRevision createFileStructureForNewApplicationFromFqn(final Session session,
      final Application application, final String[] splitFileFqn) {

    final Directory rootDir = new Directory("*");
    application.setRootDirectory(rootDir);
    session.save(application);
    return createRemainingFilePath(session, rootDir, splitFileFqn);
  }

  public FileRevision createFileStructureFromStaticData(final Session session,
      final FileIdentifier fileIdentifier, final String repoName, final String landscapeTokenId,
      final Commit commit) {
    final String[] pathSegments = fileIdentifier.getFilePath().split("/");
    String[] directorySegments = {repoName};
    if (pathSegments.length > 1) {
      directorySegments = Arrays.copyOfRange(pathSegments, 0, pathSegments.length - 2);
      directorySegments = Stream.concat(Stream.of(repoName), Arrays.stream(directorySegments))
          .toArray(String[]::new);
    }

    FileRevision file = getFileRevisionFromHash(session, fileIdentifier.getFileHash(), repoName,
        landscapeTokenId).orElse(null);
    if (file == null) {
      file = new FileRevision(fileIdentifier.getFileHash(), pathSegments[pathSegments.length - 1]);
    }

    commit.addFileRevision(file);

    final Directory parentDir =
        directoryRepository.createDirectoryStructureAndReturnLastDirStaticData(session,
            directorySegments, repoName, landscapeTokenId);
    parentDir.addFileRevision(file);

    session.save(List.of(parentDir, commit, file));

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
