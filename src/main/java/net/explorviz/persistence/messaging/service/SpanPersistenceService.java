package net.explorviz.persistence.messaging.service;

import com.google.common.collect.ObjectArrays;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import net.explorviz.persistence.avro.SpanData;
import net.explorviz.persistence.messaging.SpanDataConsumer;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Clazz;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Span;
import net.explorviz.persistence.ogm.Trace;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.ClazzRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.FunctionRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import net.explorviz.persistence.repository.SpanRepository;
import net.explorviz.persistence.repository.TraceRepository;
import org.jboss.logging.Logger;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class SpanPersistenceService {

  private static final Logger LOGGER = Logger.getLogger(SpanDataConsumer.class);

  @Inject private ApplicationRepository applicationRepository;

  @Inject private ClazzRepository clazzRepository;

  @Inject private CommitRepository commitRepository;

  @Inject private FileRevisionRepository fileRevisionRepository;

  @Inject private FunctionRepository functionRepository;

  @Inject private LandscapeRepository landscapeRepository;

  @Inject private SpanRepository spanRepository;

  @Inject private SessionFactory sessionFactory;

  @Inject private TraceRepository traceRepository;

  public void saveSpanData(final Session session, final SpanData spanData) {
    final Span span = new Span(spanData);

    if (!spanData.getParentId().isEmpty()) {
      final Span parentSpan =
          spanRepository.getOrCreateSpan(session, spanData.getParentId());
      span.setParentSpan(parentSpan);
    }

    final Trace trace = traceRepository.getOrCreateTrace(session, spanData.getTraceId());
    trace.addChildSpan(span);

    final Landscape landscape =
        landscapeRepository.getOrCreateLandscape(
            session, spanData.getLandscapeTokenId());
    landscape.addTrace(trace);

    final Function function;
    if (spanData.getCommitHash() != null) {
      function = resolveFunctionFqn(session, spanData, spanData.getCommitHash());
    } else {
      function = resolveFunctionFqn(session, spanData);
    }

    span.setFunction(function);

    session.save(List.of(span, trace, landscape, function));
  }

  private Function resolveFunctionFqn(final Session session, final SpanData spanData) {
    final String[] splitFilePath = spanData.getFilePath().split("/");
    final String functionName = spanData.getFunctionName();

    final FileRevision fileRevision = resolveFileRevision(session, spanData, splitFilePath);
    final Function function;

    if (spanData.getClassName() != null) {
      final Clazz clazz =
          clazzRepository
              .findClassByClassPathAndFileRevisionId(
                  session, spanData.getClassName().split("\\."), fileRevision.getId())
              .orElseGet(
                  () ->
                      clazzRepository.createClazzPathAndReturnLastClazz(
                          session,
                          spanData.getClassName().split("\\."),
                          fileRevision.getId()));

      function =
          functionRepository
              .findFunctionByFunctionNameAndClazzId(session, functionName, clazz.getId())
              .orElse(new Function(functionName));
      clazz.addFunction(function);
      session.save(clazz);
    } else {
      function =
          functionRepository
              .findFunctionWithFunctionNameAndFileRevisionId(
                  session, functionName, fileRevision.getId())
              .orElse(new Function(functionName));
      fileRevision.addFunction(function);
      session.save(fileRevision);
    }

    return function;
  }

  private Function resolveFunctionFqn(
      final Session session, final SpanData spanData, final String commitHash) {
    final String[] splitFilePath = spanData.getFilePath().split("/");
    final String functionName = spanData.getFunctionName();

    if (spanData.getClassName() != null) {
      return functionRepository
          .findFunction(
              session,
              spanData.getApplicationName(),
              ObjectArrays.concat(splitFilePath, functionName),
              spanData.getLandscapeTokenId(),
              commitHash,
              spanData.getClassName().split("\\."))
          .orElseGet(() -> resolveFunctionFqn(session, spanData));
    } else {
      return functionRepository
          .findFunction(
              session,
              spanData.getApplicationName(),
              ObjectArrays.concat(splitFilePath, functionName),
              commitHash,
              spanData.getLandscapeTokenId())
          .orElseGet(() -> resolveFunctionFqn(session, spanData));
    }
  }

  private FileRevision resolveFileRevision(
      final Session session, final SpanData spanData, final String[] splitFileFqn) {
    return fileRevisionRepository
        .findFileRevisionFromAppNameAndPathWithoutCommit(
            session,
            spanData.getApplicationName(),
            splitFileFqn,
            spanData.getLandscapeTokenId())
        .orElseGet(
            () -> {
              final Application application =
                  applicationRepository
                      .findApplicationByNameAndLandscapeToken(
                          session,
                          spanData.getApplicationName(),
                          spanData.getLandscapeTokenId())
                      .orElse(new Application(spanData.getApplicationName()));

              FileRevision fileRevision;

              if (application.getRootDirectory() == null) {
                // Application did not previously exist, build file structure from scratch
                fileRevision =
                    fileRevisionRepository.createFileStructureForNewApplicationFromFqn(
                        session, application, splitFileFqn);
              } else {
                // Create missing directories and file for existing Application
                fileRevision =
                    fileRevisionRepository.createFileStructureForExistingApplicationFromFileFqn(
                        session,
                        splitFileFqn,
                        application.getName(),
                        spanData.getLandscapeTokenId());
              }

              return fileRevision;
            });
  }
}
