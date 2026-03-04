package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testPersistSpanMultipleSpans() {
      String spanIdTwo = "span2";
      String functionName = "myMethod";
      String functionNameTwo = "yourMethod";
      List<String> functionFqn = List.of("net", "explorviz", baseAppName, "MyClass", functionName);
      List<String> functionFqnTwo =
          List.of("net", "explorviz", baseAppName, "YourClass", functionNameTwo);

      SpanData testSpanData = SpanData.newBuilder().setSpanId(baseSpanId).setTraceId(baseTraceId)
          .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
          .setFunctionFqn(String.join(".", functionFqn)).setStartTime(1).setEndTime(5).build();

      Empty reply = spanDataService.persistSpan(testSpanData).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      SpanData testSpanDataTwo =
          SpanData.newBuilder().setSpanId(spanIdTwo).setTraceId(baseTraceId).setParentId(baseSpanId)
              .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
              .setFunctionFqn(String.join(".", functionFqnTwo)).setStartTime(2).setEndTime(4)
              .build();

      reply = spanDataService.persistSpan(testSpanDataTwo).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      Map<String, Object> params = new HashMap<>();
      params.put("landscapeToken", landscapeToken);
      params.put("appName", baseAppName);
      params.put("traceId", baseTraceId);
      params.put("spanId", baseSpanId);
      params.put("spanId2", spanIdTwo);
      params.put("dirOne", functionFqn.get(0));
      params.put("dirTwo", functionFqn.get(1));
      params.put("dirThree", functionFqn.get(2));
      params.put("fileNameOne", functionFqn.get(3));
      params.put("fileNameTwo", functionFqnTwo.get(3));
      params.put("funName", functionName);
      params.put("funName2", functionNameTwo);


      Boolean databaseIsCorrect = session.queryForObject(Boolean.class, """
          RETURN EXISTS {
          MATCH (app:Application {name: $appName})
                -[:HAS_ROOT]->(:Directory)
                -[:CONTAINS]->(:Directory {name: $dirOne})
                -[:CONTAINS]->(:Directory {name: $dirTwo})
                -[:CONTAINS]->(:Directory {name: $dirThree})
                -[:CONTAINS]->(file:FileRevision {name: $fileNameOne})
                -[:CONTAINS]->(fun1:Function {name: $funName})
                <-[:REPRESENTS]-(span1:Span {spanId: $spanId})
                <-[:CONTAINS]-(t:Trace {traceId: $traceId})
                <-[:CONTAINS]-(:Landscape {tokenId: $landscapeToken})
          
          MATCH (file2:FileRevision {name: $fileNameTwo})-[:CONTAINS]->(fun2:Function {name: $funName2})
          MATCH (fun2)<-[:REPRESENTS]-(span2:Span {spanId: $spanId2})
                <-[:CONTAINS]-(t)
          MATCH (span2)-[:HAS_PARENT]->(span1)
          
          WHERE fun1 <> fun2
          } AS exists
          """, params);

      assertTrue(databaseIsCorrect);
    }

    @Test
    void testPersistSpanMultipleFunctionsFromOneFileRevision() {
      String spanIdTwo = "span2";
      String functionName = "myMethod";
      String functionNameTwo = "yourMethod";
      List<String> functionFqn = List.of("net", "explorviz", baseAppName, "MyClass", functionName);
      List<String> functionFqnTwo =
          List.of("net", "explorviz", baseAppName, "MyClass", functionNameTwo);

      SpanData testSpanData = SpanData.newBuilder().setSpanId(baseSpanId).setTraceId(baseTraceId)
          .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
          .setFunctionFqn(String.join(".", functionFqn)).setStartTime(1).setEndTime(5).build();

      Empty reply = spanDataService.persistSpan(testSpanData).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      SpanData testSpanDataTwo =
          SpanData.newBuilder().setSpanId(spanIdTwo).setTraceId(baseTraceId).setParentId(baseSpanId)
              .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
              .setFunctionFqn(String.join(".", functionFqnTwo)).setStartTime(2).setEndTime(4)
              .build();

      reply = spanDataService.persistSpan(testSpanDataTwo).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      Map<String, Object> params = new HashMap<>();
      params.put("landscapeToken", landscapeToken);
      params.put("appName", baseAppName);
      params.put("traceId", baseTraceId);
      params.put("spanId", baseSpanId);
      params.put("spanId2", spanIdTwo);
      params.put("dirOne", functionFqn.get(0));
      params.put("dirTwo", functionFqn.get(1));
      params.put("dirThree", functionFqn.get(2));
      params.put("dirFour", functionFqn.get(3));
      params.put("funName", functionName);
      params.put("funName2", functionNameTwo);

      Boolean databaseIsCorrect = session.queryForObject(Boolean.class, """
          RETURN EXISTS {
          MATCH (app:Application {name: $appName})
                -[:HAS_ROOT]->(:Directory)
                -[:CONTAINS]->(:Directory {name: $dirOne})
                -[:CONTAINS]->(:Directory {name: $dirTwo})
                -[:CONTAINS]->(:Directory {name: $dirThree})
                -[:CONTAINS]->(file:FileRevision {name: $dirFour})
                -[:CONTAINS]->(fun1:Function {name: $funName})
                <-[:REPRESENTS]-(span1:Span {spanId: $spanId})
                <-[:CONTAINS]-(t:Trace {traceId: $traceId})
                <-[:CONTAINS]-(:Landscape {tokenId: $landscapeToken})
          
          MATCH (file)-[:CONTAINS]->(fun2:Function {name: $funName2})
          MATCH (fun2)<-[:REPRESENTS]-(span2:Span {spanId: $spanId2})
                <-[:CONTAINS]-(t)
          MATCH (span2)-[:HAS_PARENT]->(span1)
          
          WHERE fun1 <> fun2
          } AS exists
          """, params);

      assertTrue(databaseIsCorrect);
    }

    /**
     * If span with commit is persisted and no static data are present, then the span should be
     * treated as if no commit id was attached
     */
    @Test
    void testPersistSpanWithCommitWithoutStaticData() {
      String functionName = "myMethod";
      List<String> functionFqn = List.of("net", "explorviz", baseAppName, "MyClass", functionName);
      String commitHash = "commit1";

      SpanData testSpanData = SpanData.newBuilder().setSpanId(baseSpanId).setTraceId(baseTraceId)
          .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
          .setCommitId(commitHash).setFunctionFqn(String.join(".", functionFqn)).setStartTime(1)
          .setEndTime(5).build();

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

      Commit commit = session.queryForObject(Commit.class, """
          MATCH (c:Commit {hash: $commitHash})
          RETURN c;
          """, Map.of("commitHash", commitHash));

      assertNotNull(result);
      assertNull(commit);
    }

    @Test
    void testPersistSpanWithPartOfFunctionPathAlreadyExisting() {
      String functionName = "myMethod";
      List<String> functionFqn = List.of("net", "explorviz", baseAppName, "MyClass", functionName);
      String functionNameTwo = "acting";
      List<String> functionFqnTwo =
          List.of("net", "explorviz", baseAppName, "inner", "TheClass", functionNameTwo);
      String spanIdTwo = "span2";

      SpanData testSpanData = SpanData.newBuilder().setSpanId(baseSpanId).setTraceId(baseTraceId)
          .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
          .setFunctionFqn(String.join(".", functionFqn)).setStartTime(1).setEndTime(5).build();

      SpanData testSpanDataTwo = SpanData.newBuilder().setSpanId(spanIdTwo).setTraceId(baseTraceId)
          .setApplicationName(baseAppName).setLandscapeTokenId(landscapeToken)
          .setFunctionFqn(String.join(".", functionFqnTwo)).setStartTime(1).setEndTime(5).build();

      Empty reply = spanDataService.persistSpan(testSpanData).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      reply = spanDataService.persistSpan(testSpanDataTwo).await().atMost(Duration.ofSeconds(5));
      assertNotNull(reply);

      Map<String, Object> params = new HashMap<>();
      params.put("landscapeToken", landscapeToken);
      params.put("appName", baseAppName);
      params.put("traceId", baseTraceId);
      params.put("spanId", baseSpanId);
      params.put("spanIdTwo", spanIdTwo);
      params.put("dirOne", functionFqn.get(0));
      params.put("dirTwo", functionFqn.get(1));
      params.put("dirThree", functionFqn.get(2));
      params.put("fileNameOne", functionFqn.get(3));
      params.put("dirFour", functionFqnTwo.get(3));
      params.put("fileNameTwo", functionFqnTwo.get(4));
      params.put("funName", functionName);
      params.put("funNameTwo", functionNameTwo);

      Boolean databaseIsCorrect = session.queryForObject(Boolean.class, """
          RETURN EXISTS {
          MATCH (app:Application {name: $appName})
                -[:HAS_ROOT]->(:Directory)
                -[:CONTAINS]->(:Directory {name: $dirOne})
                -[:CONTAINS]->(:Directory {name: $dirTwo})
                -[:CONTAINS]->(sharedDir:Directory {name: $dirThree})
                -[:CONTAINS]->(file:FileRevision {name: $fileNameOne})
                -[:CONTAINS]->(fun1:Function {name: $funName})
                <-[:REPRESENTS]-(span1:Span {spanId: $spanId})
                <-[:CONTAINS]-(t:Trace {traceId: $traceId})
                <-[:CONTAINS]-(:Landscape {tokenId: $landscapeToken})
          
          MATCH (sharedDir)-[:CONTAINS]->(:Directory {name: $dirFour})
                -[:CONTAINS]->(file2:FileRevision {name: $fileNameTwo})
                -[:CONTAINS]->(fun2:Function {name: $funNameTwo})
                <-[:REPRESENTS]-(span2:Span {spanId: $spanIdTwo})
                <-[:CONTAINS]-(t)
          } AS exists
          """, params);

      Result result = session.query("""
          RETURN
            COUNT {(:Directory {name: $dirOne})} AS dirOne,
            COUNT {(:Directory {name: $dirTwo})} AS dirTwo,
            COUNT {(:Directory {name: $dirThree})} AS dirThree,
            COUNT {(:Directory {name: $dirFour})} AS dirFour,
            COUNT {(:Directory)} AS directories""", params);

      Map<String, Object> countMap = result.queryResults().iterator().next();
      assertEquals(1, (Long) countMap.get("dirOne"));
      assertEquals(1, (Long) countMap.get("dirTwo"));
      assertEquals(1, (Long) countMap.get("dirThree"));
      assertEquals(1, (Long) countMap.get("dirFour"));
      assertEquals(5, (Long) countMap.get("directories"));
      assertTrue(databaseIsCorrect);
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
      application.setRootDirectory(new Directory(baseRepoName));

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
      baseDirNames = List.of("net", "explorviz", baseAppName);
      baseFileName = "MyClass";
      baseFileHash = "1";
      baseFunctionName = "myMethod";

      buildDefaultStaticData(session);
    }

    @Test
    void testPersistSpanWithoutCommitId() {
      List<String> functionFqn = new ArrayList<>(baseDirNames);
      Collections.addAll(functionFqn, baseFileName, baseFunctionName);

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
      params.put("repoRoot", functionFqn.get(0));
      params.put("dirOne", functionFqn.get(1));
      params.put("dirTwo", functionFqn.get(2));
      params.put("dirThree", functionFqn.get(3));
      params.put("dirFour", functionFqn.get(4));
      params.put("funName", baseFunctionName);
      params.put("fileHash", baseFileHash);

      Boolean databaseIsCorrect = session.queryForObject(Boolean.class, """
          RETURN EXISTS {
          MATCH (app:Application {name: $appName})
                -[:HAS_ROOT]->(:Directory {name: $repoRoot})
                -[:CONTAINS]->(:Directory {name: $dirOne})
                -[:CONTAINS]->(:Directory {name: $dirTwo})
                -[:CONTAINS]->(d:Directory {name: $dirThree})
                -[:CONTAINS]->(fileD:FileRevision {name: $dirFour})
                -[:CONTAINS]->(funD:Function {name: $funName})
                <-[:REPRESENTS]-(span:Span {spanId: $spanId})
                <-[:CONTAINS]-(:Trace {traceId: $traceId})
                <-[:CONTAINS]-(:Landscape {tokenId: $landscapeToken})
          
          MATCH (d)-[:CONTAINS]->(fileS:FileRevision {name: $dirFour, hash: $fileHash})
                -[:CONTAINS]->(funS:Function {name: $funName})
          
          WHERE NOT EXISTS { MATCH (:Span)-[:REPRESENTS]->(funS) }
            AND fileD.hash IS NULL
            AND funS <> funD
          } as exists;
          """, params);

      assertNotNull(databaseIsCorrect);
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

      Integer functionCount = session.queryForObject(Integer.class,"""
          RETURN
            COUNT {(:Function)} AS function""", params);

      assertEquals(1, functionCount);
      assertNotNull(foundCommit);
    }

    @Test
    void testPersistSpanMultipleFunctionsFromOneFileRevisionWithDifferentCommitIds() {

    }
  }

}
