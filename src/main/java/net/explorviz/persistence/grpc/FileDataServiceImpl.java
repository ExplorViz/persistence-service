package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import net.explorviz.persistence.ogm.Clazz;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.proto.ClassData;
import net.explorviz.persistence.proto.FileData;
import net.explorviz.persistence.proto.FileDataService;
import net.explorviz.persistence.proto.FunctionData;
import net.explorviz.persistence.repository.ClassNodeRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.FunctionRepository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class FileDataServiceImpl implements FileDataService {

  @Inject
  private ClassNodeRepository classNodeRepository;

  @Inject
  private FileRevisionRepository fileRevisionRepository;

  @Inject
  private FunctionRepository functionRepository;

  @Inject
  private SessionFactory sessionFactory;

  @Blocking
  @Override
  public Uni<Empty> sendFileData(final FileData request) {
    final Session session = sessionFactory.openSession();

    final FileRevision file =
        fileRevisionRepository.getFileRevisionFromHash(session, request.getFileHash(),
            request.getRepositoryName(), request.getLandscapeToken()).orElse(null);

    if (file == null) {
      return Uni.createFrom().failure(Status.FAILED_PRECONDITION.withDescription(
          "No corresponding file was sent before in CommitData.").asRuntimeException());
    }

    file.setLanguage(request.getLanguage());
    file.setPackageName(request.getPackageName());
    for (final String importName : request.getImportNamesList()) {
      file.addImportNames(importName);
    }
    request.getMetricsMap().forEach(file::addMetric);
    file.setLastEditor(request.getLastEditor());
    file.setAddedLines(request.getAddedLines());
    file.setModifiedLines(request.getModifiedLines());
    file.setDeletedLines(request.getDeletedLines());

    for (final ClassData c : request.getClassesList()) {
      final Clazz clazz = createClazz(session, c, request);
      file.addClass(clazz);
    }

    for (final FunctionData f : request.getFunctionsList()) {
      final Function function = new Function(f);
      file.addFunction(function);
    }

    file.setHasFileData(true);

    session.save(file);

    return Uni.createFrom().item(Empty.getDefaultInstance());
  }

  private Clazz createClazz(final Session session, final ClassData classData,
      final FileData request) {
    return classNodeRepository.findClassByLandscapeTokenAndRepositoryAndFileHash(session,
            request.getLandscapeToken(), request.getRepositoryName(), request.getFileHash())
        .orElseGet(() -> {
          final Clazz clazz = new Clazz(classData);

          // TODO: Handle fields

          for (final ClassData c : classData.getInnerClassesList()) {
            final Clazz innerClass = createClazz(session, c, request);
            clazz.addInnerClass(innerClass);
          }

          for (final FunctionData f : classData.getFunctionsList()) {
            // Create new function, since classNode was newly created
            final Function function = new Function(f);
            clazz.addFunctions(function);
          }

          session.save(clazz);

          return clazz;
        });
  }
}
