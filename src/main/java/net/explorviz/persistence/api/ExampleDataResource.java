package net.explorviz.persistence.api;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Branch;
import net.explorviz.persistence.ogm.Clazz;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import net.explorviz.persistence.ogm.Span;
import net.explorviz.persistence.ogm.Trace;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

/**
 * Contains dev-exclusive endpoints for populating the database with testing data without having to
 * run other ExplorViz services. Simply cURL endpoints or access them via browser.
 */
@IfBuildProfile("dev")
@Path("/test")
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseObjectForClearerAPI", "PMD.NcssCount"})
class ExampleDataResource {

  @Inject
  private SessionFactory sessionFactory;

  @GET
  @Path("/trace")
  public void createTestingDynamicData() {
    final Session session = sessionFactory.openSession();
    session.query("""
        MERGE (l:Landscape {tokenId: "mytokenvalue"})
        MERGE (l)-[:CONTAINS]->(t1:Trace {traceId: "trace1"})
        MERGE (l)-[:CONTAINS]->(t2:Trace {traceId: "trace2"})
        SET t1.startTime = 1000000000, t1.endTime = 1001000000
        SET t2.startTime = 2000000000, t2.endTime = 4002800000
        MERGE (t1)-[:CONTAINS]->(s1:Span {spanId: "span1"})
        MERGE (t2)-[:CONTAINS]->(s2:Span {spanId: "span2"})
        MERGE (t2)-[:CONTAINS]->(s3:Span {spanId: "span3"})-[:HAS_PARENT]->(s2)
        MERGE (t2)-[:CONTAINS]->(s4:Span {spanId: "span4"})-[:HAS_PARENT]->(s3)
        SET s1.startTime = 1000000000, s1.endTime = 1001000000
        SET s2.startTime = 2000000000, s2.endTime = 2003000000
        SET s3.startTime = 2500000000, s3.endTime = 3002900000
        SET s4.startTime = 3000000000, s4.endTime = 4002800000
        
        MERGE (app1:Application {name: "hello-world1"})
        MERGE (app1)-[:HAS_ROOT]->(appRoot:Directory {name: "hello-world"})
        MERGE (appRoot)
          -[:CONTAINS]->(:Directory {name: "net"})
          -[:CONTAINS]->(:Directory {name: "explorviz"})
          -[:CONTAINS]->(outerDir:Directory {name: "helloworld"})
          -[:CONTAINS]->(innerDir:Directory {name: "innerpackage"})
        MERGE (outerDir)-[:CONTAINS]->(file1:FileRevision {name: "File1.java"})
        MERGE (outerDir)-[:CONTAINS]->(file2:FileRevision {name: "File2.java"})
        MERGE (innerDir)-[:CONTAINS]->(file3:FileRevision {name: "File3.java"})
        MERGE (file1)-[:CONTAINS]->(func1:Function {name: "function1"})
        MERGE (file2)-[:CONTAINS]->(func2:Function {name: "function2"})
        MERGE (file3)-[:CONTAINS]->(func3:Function {name: "function3"})
        MERGE (l)
          -[:CONTAINS]->(r1:Repository {name: "repo1"})
          -[:CONTAINS]->(:Commit {hash: "commit1"})
          -[:CONTAINS]->(file1)
        MERGE (r1)
          -[:HAS_ROOT]->(outerDir)
          
        MERGE (app2:Application {name: "hello-world2"})
        MERGE (app2)-[:HAS_ROOT]->(outerDir)
        
        MERGE (s1)-[:REPRESENTS]->(func1)
        MERGE (s2)-[:REPRESENTS]->(func2)
        MERGE (s3)-[:REPRESENTS]->(func3)<-[:REPRESENTS]-(s4);
        """, Map.of());
  }

  @GET
  @Path("/repo")
  public void createTestingRepository() {
    final Landscape landscape = new Landscape("mytokenvalue");

    final Repository repository = new Repository("hello-world");
    landscape.addRepository(repository);

    final Branch branch1 = new Branch("main");
    final Branch branch2 = new Branch("feature-a");
    repository.addBranch(branch1);
    repository.addBranch(branch2);

    final Commit commit1 = new Commit("commit1");
    final Commit commit2 = new Commit("commit2");
    final Commit commit3 = new Commit("commit3");
    commit1.setBranch(branch1);
    commit2.setBranch(branch1);
    commit2.addParent(commit1);
    commit3.setBranch(branch2);
    commit3.addParent(commit1);
    repository.addCommit(commit1);
    repository.addCommit(commit2);
    repository.addCommit(commit3);

    final Application application = new Application("hello-world");

    Directory currentDir = new Directory("hello-world");
    repository.setRootDirectory(currentDir);
    application.setRootDirectory(currentDir);

    final String[] dirNames = {"net", "explorviz", "helloworld"};
    for (final String dirName : dirNames) {
      final Directory newDir = new Directory(dirName);
      currentDir.addSubdirectory(newDir);
      currentDir = newDir;
    }
    final Directory innerDir = new Directory("innerpackage");
    currentDir.addSubdirectory(innerDir);

    final FileRevision fileA = new FileRevision("ClassA.java");
    final FileRevision fileB = new FileRevision("ClassB.java");
    final FileRevision fileC = new FileRevision("ClassC.java");
    final FileRevision fileD = new FileRevision("ClassD.java");
    final FileRevision fileModified = new FileRevision("ClassD.java");
    currentDir.addFileRevision(fileA);
    currentDir.addFileRevision(fileB);
    innerDir.addFileRevision(fileC);
    innerDir.addFileRevision(fileD);
    innerDir.addFileRevision(fileModified);
    commit1.addFileRevision(fileA);
    commit2.addFileRevision(fileA);
    commit2.addFileRevision(fileB);
    commit2.addFileRevision(fileD);
    commit3.addFileRevision(fileB);
    commit3.addFileRevision(fileC);
    commit3.addFileRevision(fileModified);
    List.of(fileA, fileB, fileC, fileD, fileModified).forEach(f -> {
      addFunctionsToFile(f);
      addRandomFileMetrics(f);
    });

    final Clazz classA = new Clazz("ClassA");
    final Clazz classB = new Clazz("ClassB");
    final Clazz classC = new Clazz("ClassC");
    final Clazz classD = new Clazz("ClassD");
    final Clazz classModified = new Clazz("ClassD");
    fileA.addClass(classA);
    fileB.addClass(classB);
    fileC.addClass(classC);
    fileD.addClass(classD);
    fileModified.addClass(classModified);
    List.of(classA, classB, classC, classD, classModified).forEach(c -> {
      addFunctionsToClass(c);
      addRandomClassMetrics(c);
    });

    final Session session = sessionFactory.openSession();
    session.save(List.of(landscape, application));
  }

  @GET
  @Path("/monorepo")
  public void createTestingMonorepo() {
    final Landscape landscape = new Landscape("mytokenvalue");

    final Repository repository = new Repository("monorepo");
    landscape.addRepository(repository);

    final Branch branch1 = new Branch("main");
    final Branch branch2 = new Branch("feature-a");
    repository.addBranch(branch1);
    repository.addBranch(branch2);

    final Commit commit1 = new Commit("commit1");
    final Commit commit2 = new Commit("commit2");
    final Commit commit3 = new Commit("commit3");
    commit1.setBranch(branch1);
    commit2.setBranch(branch1);
    commit2.addParent(commit1);
    commit3.setBranch(branch2);
    commit3.addParent(commit1);
    repository.addCommit(commit1);
    repository.addCommit(commit2);
    repository.addCommit(commit3);

    final Application application1 = new Application("app-one");
    final Application application2 = new Application("app-two");

    final Directory repoRoot = new Directory("monorepo");
    repository.setRootDirectory(repoRoot);
    repoRoot.addFileRevision(new FileRevision("README.md"));

    Directory appOneDir = new Directory("app-one");
    Directory appTwoDir = new Directory("app-two");

    application1.setRootDirectory(appOneDir);
    application2.setRootDirectory(appTwoDir);

    repoRoot.addSubdirectory(appOneDir);
    repoRoot.addSubdirectory(appTwoDir);

    final String[] appOneDirNames = {"net", "explorviz", "appone"};
    final String[] appTwoDirNames = {"net", "explorviz", "apptwo"};

    for (final String dirName : appOneDirNames) {
      final Directory newDir = new Directory(dirName);
      appOneDir.addSubdirectory(newDir);
      appOneDir = newDir;
    }

    for (final String dirName : appTwoDirNames) {
      final Directory newDir = new Directory(dirName);
      appTwoDir.addSubdirectory(newDir);
      appTwoDir = newDir;
    }

    final FileRevision fileA1 = new FileRevision("ClassA.java");
    final FileRevision fileB1 = new FileRevision("ClassB.java");
    final FileRevision fileB1Modified = new FileRevision("ClassB.java");
    final FileRevision fileA2 = new FileRevision("ClassA.java");
    final FileRevision fileB2 = new FileRevision("ClassB.java");
    appOneDir.addFileRevision(fileA1);
    appOneDir.addFileRevision(fileB1);
    appOneDir.addFileRevision(fileB1Modified);
    appTwoDir.addFileRevision(fileA2);
    appTwoDir.addFileRevision(fileB2);
    commit1.addFileRevision(fileA1);
    commit1.addFileRevision(fileA2);
    commit1.addFileRevision(fileB1);
    commit1.addFileRevision(fileB2);
    commit2.addFileRevision(fileA1);
    commit2.addFileRevision(fileA2);
    commit3.addFileRevision(fileA1);
    commit3.addFileRevision(fileB1Modified);
    commit3.addFileRevision(fileA2);
    commit3.addFileRevision(fileB2);
    List.of(fileA1, fileA2, fileB1, fileB2).forEach(f -> {
      addFunctionsToFile(f);
      addRandomFileMetrics(f);
    });

    final Session session = sessionFactory.openSession();
    session.save(List.of(landscape, application1, application2));
  }

  @GET
  @Path("/purge")
  public void purgeDatabase() {
    final Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }

  private void addFunctionsToFile(final FileRevision fileRevision) {
    fileRevision.addFunction(new Function("doSomething"));
    fileRevision.addFunction(new Function("findObject"));
    fileRevision.addFunction(new Function("tryMyBest"));
  }

  private void addFunctionsToClass(final Clazz clazz) {
    clazz.addFunction(new Function("performClassMethod"));
    clazz.addFunction(new Function("encapsulateParent"));
    clazz.addFunction(new Function("inheritInterface"));
  }

  private void addRandomFileMetrics(final FileRevision fileRevision) {
    fileRevision.addMetric("LCOM4", Math.floor(Math.random() * 5));
    fileRevision.addMetric("loc", Math.floor(Math.random() * 250));
    fileRevision.addMetric("cyclomatic_complexity", Math.floor(Math.random() * 10));
    fileRevision.addMetric("cyclomatic_complexity_weighted", Math.floor(Math.random() * 10));
  }

  private void addRandomClassMetrics(final Clazz clazz) {
    clazz.addMetric("LCOM4", Math.floor(Math.random() * 5));
    clazz.addMetric("loc", Math.floor(Math.random() * 250));
    clazz.addMetric("cyclomatic_complexity", Math.floor(Math.random() * 10));
    clazz.addMetric("cyclomatic_complexity_weighted", Math.floor(Math.random() * 10));
  }

  private void addRandomSpan(final Trace trace, final String name) {
    final Span span = new Span(name);
    final long randNumb = (long)(Math.random() * 100_000_000_000.0);
    span.setStartTime(randNumb);
    span.setEndTime(randNumb + 1);
    trace.addChildSpan(span);
  }

  @GET
  @Path("/timestamp")
  public void createTestingTimestamps(){
    final Landscape landscape = new Landscape("mytokenvalue");

    final Trace trace1 = new Trace("trace1");
    final Trace trace2 = new Trace("trace2");

    for(int i = 0; i < 5; i++){
      addRandomSpan(trace1, "trace1_span" + i);
      addRandomSpan(trace2, "trace2_span" + i);
    }

    landscape.addTrace(trace1);
    landscape.addTrace(trace2);

    final Session session = sessionFactory.openSession();
    session.save(List.of(landscape));
  }
}

