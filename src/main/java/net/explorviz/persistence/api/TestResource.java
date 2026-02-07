package net.explorviz.persistence.api;

import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.explorviz.persistence.ogm.Commit;
import net.explorviz.persistence.ogm.Directory;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Repository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@IfBuildProfile("dev")
@Path("/test")
class TestResource {

  @Inject
  SessionFactory sessionFactory;

  @POST
  @Path("/repo")
  public void createTestRepository() {
    Session session = sessionFactory.openSession();

    Directory currentDir = new Directory("hello-world");

    Commit commit1 = new Commit("commit1");

    Repository repository = new Repository("hello-world");
    repository.addRootDirectory(currentDir);
    repository.addCommit(commit1);

    Landscape landscape = new Landscape("mytokenvalue");
    landscape.addRepository(repository);

    String[] dirNames = new String[] {"net", "explorviz", "helloworld"};
    for (String dirName : dirNames) {
      Directory newDir = new Directory(dirName);
      currentDir.addSubdirectory(newDir);
      currentDir = newDir;
    }

    FileRevision fileA = new FileRevision("ClassA.java");
    FileRevision fileB = new FileRevision("ClassB.java");
    FileRevision fileC = new FileRevision("ClassC.java");

    currentDir.addFileRevision(fileA);
    currentDir.addFileRevision(fileB);
    Directory packageDir = new Directory("innerpackage");
    packageDir.addFileRevision(fileC);
    currentDir.addSubdirectory(packageDir);

    commit1.addFileRevision(fileA);
    commit1.addFileRevision(fileB);
    commit1.addFileRevision(fileC);

    session.save(landscape);
  }
}
