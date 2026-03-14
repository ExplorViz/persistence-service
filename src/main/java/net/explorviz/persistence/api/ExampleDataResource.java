package net.explorviz.persistence.api;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;
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

import static com.sun.jndi.ldap.LdapPoolManager.trace;

@IfBuildProfile("dev")
@Path("/test")
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseObjectForClearerAPI", "PMD.NcssCount"})
class ExampleDataResource {

  @Inject
  private SessionFactory sessionFactory;

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
    final long randNumb = (long)(Math.random() * 100000000000.0) + 1000000000000000000L;
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
      addRandomSpan(trace1, "span" + i);
      addRandomSpan(trace2, "span" + i);
    }

    final Session session = sessionFactory.openSession();
    session.save(List.of(landscape));
  }
}

