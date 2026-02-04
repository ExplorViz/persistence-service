package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import net.explorviz.persistence.ogm.Directory;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;

@ApplicationScoped
public class DirectoryRepository {
  private static final String FIND_LONGEST_PATH_MATCH_STATIC_DATA = """
      WITH $pathSegments AS pathSegments
      MATCH (l:Landscape {tokenId: $tokenId})
        -[:CONTAINS]->(r:Repository {name: $repoName})
        -[:HAS_ROOT]->(rd:Directory {name: $repoName})
      OPTIONAL MATCH p=(rd)-[:CONTAINS]->*(:Directory)
      WHERE all(
        j in range(0,length(p))
        WHERE nodes(p)[j].name = pathSegments[j]
      )
      RETURN coalesce(last(nodes(p)), rd) AS existingDir,
             pathSegments[coalesce(length(p)+1, 0)..] AS remainingPath
      ORDER BY size(nodes(p)) DESC
      LIMIT 1;
      """;

  private Map<String, Object> findLongestPathMatchStaticData(final Session session,
      final String[] pathSegments, final String repoName, final String landscapeTokenId) {
    final Result result = session.query(FIND_LONGEST_PATH_MATCH_STATIC_DATA,
        Map.of("tokenId", landscapeTokenId, "repoName", repoName, "pathSegments", pathSegments));

    final Iterator<Map<String, Object>> resultIterator = result.queryResults().iterator();
    if (!resultIterator.hasNext()) {
      throw new NoSuchElementException("resultIterator empty");
    }

    return resultIterator.next();
  }

  public Directory createDirectoryStructureAndReturnLastDirStaticData(final Session session,
      final String[] filePath, final String repoName, final String landscapeTokenId) {
    final Map<String, Object> resultMap =
        findLongestPathMatchStaticData(session, filePath, repoName, landscapeTokenId);
    final Directory existingDir =
        resultMap.get("existingDir") instanceof Directory dir ? dir : null;
    if (existingDir == null) {
      throw new NoSuchElementException("No existing directory found");
    }

    final String[] remainingPath =
        resultMap.get("remainingPath") instanceof String[] rp ? rp : new String[0];

    Directory lastDir = existingDir;
    for (final String dirName : remainingPath) {
      Directory nextDir = null;
      for (final Directory sub : lastDir.getSubdirectories()) {
        if (sub.getName().equals(dirName)) {
          nextDir = sub;
          break;
        }
      }

      if (nextDir == null) {
        nextDir = new Directory(dirName);
        lastDir.addSubdirectory(nextDir);
      }
      lastDir = nextDir;
    }
    session.save(existingDir);

    return lastDir;
  }
}
