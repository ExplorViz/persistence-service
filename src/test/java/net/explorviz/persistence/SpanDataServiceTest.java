package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.proto.SpanData;
import net.explorviz.persistence.proto.SpanDataService;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class SpanDataServiceTest {
  @GrpcClient
  SpanDataService spanDataService;

  @Inject
  CommitRepository commitRepository;

  @Inject
  FileRevisionRepository fileRevisionRepository;

  @Inject
  LandscapeRepository landscapeRepository;

  @Inject
  SessionFactory sessionFactory;

  private Session session;
  private String landscapeToken;
  private String baseTraceId;
  private String baseSpanId;
  private String baseAppName;

  @BeforeEach
  void cleanup() {
    session = sessionFactory.openSession();
    session.purgeDatabase();

    landscapeToken = "mytokenvalue";
    baseTraceId = "myTrace";
    baseSpanId = "mySpan";
    baseAppName = "myApp";
  }

  @Nested
  class WithoutStaticData {
    @Test
    void testPersistSpan() {
      String functionName = "myMethod";
      List<String> functionFqn = List.of("net", "explorviz", baseAppName, "MyClass", functionName);

      SpanData testSpanData = SpanData.newBuilder().setSpanId(baseSpanId).setTraceId(baseTraceId)
          .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
          .setFunctionFqn(String.join(".", functionFqn)).setStartTime(1).setEndTime(5).build();

      Empty reply = spanDataService.persistSpan(testSpanData).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      Map<String, Object> params = new HashMap<>();
      params.put("landscapeToken", landscapeToken);
      params.put("appName", baseAppName);
      params.put("traceId", baseTraceId);
      params.put("spanId", baseSpanId);
      params.put("dirOne", functionFqn.get(0));
      params.put("dirTwo", functionFqn.get(1));
      params.put("dirThree", functionFqn.get(2));
      params.put("dirFour", functionFqn.get(3));
      params.put("funName", functionName);

      Application result = session.queryForObject(Application.class, """
          MATCH (app:Application {name: $appName})
                -[:HAS_ROOT]->(:Directory)
                -[:CONTAINS]->(:Directory {name: $dirOne})
                -[:CONTAINS]->(:Directory {name: $dirTwo})
                -[:CONTAINS]->(:Directory {name: $dirThree})
                -[:CONTAINS]->(:FileRevision {name: $dirFour})
                -[:CONTAINS]->(:Function {name: $funName})
                <-[:REPRESENTS]-(:Span {spanId: $spanId})
                <-[:CONTAINS]-(:Trace {traceId: $traceId})
                <-[:CONTAINS]-(:Landscape {tokenId: $landscapeToken})
          RETURN app;""", params);

      assertNotNull(result);
    }

    /**
     * Persisting the same span twice should create no additional nodes.
     */
    @Test
    void testPersistSpanIdempotent() {
      String functionName = "myMethod";
      List<String> functionFqn = List.of("net", "explorviz", baseAppName, "MyClass", functionName);

      SpanData testSpanData = SpanData.newBuilder().setSpanId(baseSpanId).setTraceId(baseTraceId)
          .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
          .setFunctionFqn(String.join(".", functionFqn)).setStartTime(1).setEndTime(5).build();

      Empty reply = spanDataService.persistSpan(testSpanData).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      reply = spanDataService.persistSpan(testSpanData).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      Result result = session.query("""
          RETURN
            COUNT {(:FileRevision)} AS files,
            COUNT {(:Application)} AS apps,
            COUNT {(:Span)} AS spans,
            COUNT {(:Trace)} AS traces""", Map.of());

      Map<String, Object> countMap = result.queryResults().iterator().next();
      assertEquals(1, (Long) countMap.get("files"));
      assertEquals(1, (Long) countMap.get("apps"));
      assertEquals(1, (Long) countMap.get("spans"));
      assertEquals(1, (Long) countMap.get("traces"));
    }
  }


  @Nested
  class WithStaticData {
    private String baseRepoName;
    private String baseBranchName;
    private String baseCommitHash;
    private List<String> baseDirNames;
    private String baseFileName;
    private String baseFileHash;
    private String baseFunctionName;

    private void buildDefaultStaticData(Session session) {
      Branch branch = new Branch(baseBranchName);
      Repository repository = new Repository(baseRepoName);
      repository.addBranch(branch);
      Landscape landscape = new Landscape(landscapeToken);
      landscape.addRepository(repository);
      Application application = new Application(baseAppName);
      application.setRootDirectory(new Directory(baseAppName));

      Directory currentDir = application.getRootDirectory();
      repository.addRootDirectory(currentDir);
      for (String dirName : baseDirNames) {
        Directory newDir = new Directory(dirName);
        currentDir.addSubdirectory(newDir);
        currentDir = newDir;
      }

      FileRevision file = new FileRevision(baseFileName);
      currentDir.addFileRevision(file);
      file.addFunction(new Function(baseFunctionName));
      file.setHash(baseFileHash);

      Commit commit = new Commit(baseCommitHash);
      repository.addCommit(commit);
      commit.addFileRevision(file);
      commit.setBranch(branch);
      landscape.addRepository(repository);

      session.save(List.of(landscape, application));
    }

    @BeforeEach
    void init() {
      baseRepoName = "myrepo";
      baseBranchName = "main";
      baseCommitHash = "commit1";
      baseDirNames = List.of("net", "explorviz", "helloworld");
      baseFileName = "MyClass";
      baseFileHash = "1";
      baseFunctionName = "myMethod";

      buildDefaultStaticData(session);
    }

    /**
     * If a commit hash is included in the span and the corresponding commit, file and function
     * nodes exist, then the span should be connected to the existing function node.
     */
    @Test
    void testPersistSpanWithCommitAndStaticDataExists() {
      List<String> functionFqn = new ArrayList<>(baseDirNames);
      Collections.addAll(functionFqn, baseFileName, baseFunctionName);

      SpanData testSpanData = SpanData.newBuilder().setSpanId(baseSpanId).setTraceId(baseTraceId)
          .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
          .setFunctionFqn(String.join(".", functionFqn)).setStartTime(1).setEndTime(5)
          .setCommitId(baseCommitHash).build();

      Empty reply = spanDataService.persistSpan(testSpanData).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      Map<String, Object> params = new HashMap<>();
      params.put("landscapeToken", landscapeToken);
      params.put("appName", baseAppName);
      params.put("traceId", baseTraceId);
      params.put("spanId", baseSpanId);
      params.put("dirOne", functionFqn.get(0));
      params.put("dirTwo", functionFqn.get(1));
      params.put("dirThree", functionFqn.get(2));
      params.put("fileName", baseFileName);
      params.put("fileHash", baseFileHash);
      params.put("funName", baseFunctionName);
      params.put("commitHash", baseCommitHash);

      Commit foundCommit = session.queryForObject(Commit.class, """
          MATCH (:Application {name: $appName})
                -[:HAS_ROOT]->(:Directory)
                -[:CONTAINS]->(:Directory {name: $dirOne})
                -[:CONTAINS]->(:Directory {name: $dirTwo})
                -[:CONTAINS]->(:Directory {name: $dirThree})
                -[:CONTAINS]->(file:FileRevision {name: $fileName, hash: $fileHash})
                -[:CONTAINS]->(:Function {name: $funName})
                <-[:REPRESENTS]-(:Span {spanId: $spanId})
                <-[:CONTAINS]-(:Trace {traceId: $traceId})
                <-[:CONTAINS]-(:Landscape {tokenId: $landscapeToken})
          MATCH (commit:Commit {hash: $commitHash})-[:CONTAINS]->(file)
          RETURN commit;""", params);

      assertNotNull(foundCommit);
    }
  }

}
