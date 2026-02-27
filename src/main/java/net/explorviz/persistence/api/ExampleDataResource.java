package net.explorviz.persistence.api;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
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
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@IfBuildProfile("dev")
@Path("/test")
@SuppressWarnings({"PMD.UseObjectForClearerAPI", "PMD.NcssCount"})
class ExampleDataResource {

  @Inject
  private SessionFactory sessionFactory;

  @POST
  @Path("/repo")
  public void createTestingRepository() {
    final Branch branch1 = new Branch("main");
    final Branch branch2 = new Branch("feature-a");

    final Commit commit1 = new Commit("commit1");
    final Commit commit2 = new Commit("commit2");
    final Commit commit3 = new Commit("commit3");

    commit2.addParent(commit1);
    commit3.addParent(commit1);

    commit1.setBranch(branch1);
    commit2.setBranch(branch1);
    commit3.setBranch(branch2);

    Directory currentDir = new Directory("hello-world");

    final Repository repository = new Repository("hello-world");
    repository.addRootDirectory(currentDir);
    repository.addCommit(commit1);
    repository.addCommit(commit2);
    repository.addCommit(commit3);
    repository.addBranch(branch1);
    repository.addBranch(branch2);

    final Application application = new Application("hello-world");
    application.setRootDirectory(currentDir);

    final Landscape landscape = new Landscape("mytokenvalue");
    landscape.addRepository(repository);

    final String[] dirNames = {"net", "explorviz", "helloworld"};
    for (final String dirName : dirNames) {
      final Directory newDir = new Directory(dirName);
      currentDir.addSubdirectory(newDir);
      currentDir = newDir;
    }

    final FileRevision fileA = new FileRevision("ClassA.java");
    final FileRevision fileB = new FileRevision("ClassB.java");
    final FileRevision fileC = new FileRevision("ClassC.java");

    final Clazz classA = new Clazz("ClassA");
    final Clazz classB = new Clazz("ClassB");
    final Clazz classC = new Clazz("ClassC");

    classA.addMetric("LCOM4", 1d);
    classA.addMetric("loc", 120d);
    classA.addMetric("cyclomatic_complexity", 7d);
    classA.addMetric("cyclomatic_complexity_weighted", 5d);
    classB.addMetric("LCOM4", 2d);
    classC.addMetric("LCOM4", 3d);

    fileA.addClass(classA);
    fileB.addClass(classB);
    fileC.addClass(classC);

    fileA.addFunction(new Function("doSomethingA"));
    fileA.addFunction(new Function("doSomethingDifferentA"));

    fileB.addFunction(new Function("doSomethingB"));

    classC.addFunction(new Function("doSomethingClassC"));

    currentDir.addFileRevision(fileA);
    currentDir.addFileRevision(fileB);
    final Directory packageDir = new Directory("innerpackage");
    packageDir.addFileRevision(fileC);
    currentDir.addSubdirectory(packageDir);

    commit1.addFileRevision(fileA);

    commit2.addFileRevision(fileA);
    commit2.addFileRevision(fileB);

    commit3.addFileRevision(fileB);
    commit3.addFileRevision(fileC);

    final Session session = sessionFactory.openSession();
    session.save(List.of(landscape, application));
  }

  @POST
  @Path("/purge")
  public void purgeDatabase() {
    final Session session = sessionFactory.openSession();
    session.purgeDatabase();
  }
}
