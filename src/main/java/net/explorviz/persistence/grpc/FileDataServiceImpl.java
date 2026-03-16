package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import net.explorviz.persistence.ogm.Clazz;
import net.explorviz.persistence.ogm.Field;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.proto.ClassData;
import net.explorviz.persistence.proto.FileData;
import net.explorviz.persistence.proto.FileDataService;
import net.explorviz.persistence.repository.ClazzRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.FunctionRepository;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class FileDataServiceImpl implements FileDataService {

  @Inject private ClazzRepository clazzRepository;

  @Inject private FileRevisionRepository fileRevisionRepository;

  @Inject private FunctionRepository functionRepository;

  @Inject private SessionFactory sessionFactory;

  @Blocking
  @Override
  public Uni<Empty> persistFile(final FileData request) {
    final Session session = sessionFactory.openSession();

    final FileRevision file =
        fileRevisionRepository
            .getFileRevisionFromHashAndPath(
                session,
                request.getFileHash(),
                request.getRepositoryName(),
                request.getLandscapeToken(),
                request.getFilePath().split("/"))
            .orElseThrow(
                () ->
                    Status.FAILED_PRECONDITION
                        .withDescription("No corresponding file was sent before in CommitData.")
                        .asRuntimeException());

    file.setLanguage(request.getLanguage());
    file.setPackageName(request.getPackageName());
    request.getImportNamesList().forEach(file::addImportNames);
    request.getMetricsMap().forEach(file::addMetric);
    file.setLastEditor(request.getLastEditor());
    file.setAddedLines(request.getAddedLines());
    file.setModifiedLines(request.getModifiedLines());
    file.setDeletedLines(request.getDeletedLines());

    for (final ClassData c : request.getClassesList()) {
      try {
        file.addClass(createClazz(session, c, request));
      } catch (IllegalArgumentException e) {
        return Uni.createFrom()
            .failure(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
      }
    }

    request
        .getFunctionsList()
        .forEach(
            f -> {
              final Function function = new Function(f);
              function.addParameters(f.getParametersList());
              file.addFunction(function);
            });

    file.setHasFileData(true);

    session.save(file);

    return Uni.createFrom().item(Empty.getDefaultInstance());
  }

  private Clazz createClazz(
      final Session session, final ClassData classData, final FileData request) {
    return clazzRepository
        .findClassByLandscapeTokenAndRepositoryAndFileHashAndClazzName(
            session,
            request.getLandscapeToken(),
            request.getRepositoryName(),
            request.getFileHash(),
            classData.getName())
        .orElseGet(
            () -> {
              final Clazz clazz =
                  clazzRepository
                      .findClassFromInheritingClass(
                          session,
                          request.getLandscapeToken(),
                          request.getRepositoryName(),
                          classData.getName())
                      .map(
                          foundClazz -> {
                            if (foundClazz.getType() == null) {
                              foundClazz.setType(classData.getType());
                              foundClazz.setModifiers(classData.getModifiersList());
                              foundClazz.setImplementedInterfaces(
                                  classData.getImplementedInterfacesList());
                              foundClazz.setAnnotations(classData.getAnnotationsList());
                              foundClazz.setEnumValues(classData.getEnumValuesList());
                              foundClazz.setMetrics(classData.getMetricsMap());
                            }
                            return foundClazz;
                          })
                      /*
                       If found clazz has a type, then it must be from another commit,
                       therefore we create a new Clazz object. Same for clazz is null.
                      */
                      .orElse(new Clazz(classData));

              classData
                  .getFieldsList()
                  .forEach(
                      f ->
                          clazz.addField(
                              new Field(f.getName(), f.getType(), f.getModifiersList())));

              classData
                  .getInnerClassesList()
                  .forEach(c -> clazz.addInnerClass(createClazz(session, c, request)));

              classData
                  .getFunctionsList()
                  .forEach(
                      f -> {
                        final Function function = new Function(f);
                        function.addParameters(f.getParametersList());
                        clazz.addFunction(function);
                      });

              classData
                  .getSuperclassesList()
                  .forEach(
                      superFqn -> {
                        final String[] splitSuperFqn = superFqn.split("::");
                        clazz.addSuperClass(
                            clazzRepository
                                .findClassByLandscapeTokenAndRepositoryAndClazzFqn(
                                    session,
                                    request.getLandscapeToken(),
                                    request.getRepositoryName(),
                                    splitSuperFqn)
                                .orElse(new Clazz(splitSuperFqn[1])));
                      });

              session.save(clazz);

              return clazz;
            });
  }
}
