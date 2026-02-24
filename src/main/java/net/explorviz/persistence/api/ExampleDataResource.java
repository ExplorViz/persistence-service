package net.explorviz.persistence.api;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.List;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@IfBuildProfile("dev")
@Path("/test")
class ExampleDataResource {

  @Inject
  private SessionFactory sessionFactory;

  @POST
  @Path("/repo")
  public void createTestingRepository() {
    Directory currentDir = new Directory("hello-world");

    final Commit commit1 = new Commit("commit1");

    final Repository repository = new Repository("hello-world");
    repository.addRootDirectory(currentDir);
    repository.addCommit(commit1);

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

    currentDir.addFileRevision(fileA);
    currentDir.addFileRevision(fileB);
    final Directory packageDir = new Directory("innerpackage");
    packageDir.addFileRevision(fileC);
    currentDir.addSubdirectory(packageDir);

    commit1.addFileRevision(fileA);
    commit1.addFileRevision(fileB);
    commit1.addFileRevision(fileC);

    final Session session = sessionFactory.openSession();
    session.save(List.of(landscape, application));
  }
}
