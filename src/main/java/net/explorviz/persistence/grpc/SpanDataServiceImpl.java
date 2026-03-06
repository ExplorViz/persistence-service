package net.explorviz.persistence.grpc;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Function;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.ogm.Span;
import net.explorviz.persistence.ogm.Trace;
import net.explorviz.persistence.proto.SpanData;
import net.explorviz.persistence.proto.SpanDataService;
import net.explorviz.persistence.repository.ApplicationRepository;
import net.explorviz.persistence.repository.CommitRepository;
import net.explorviz.persistence.repository.FileRevisionRepository;
import net.explorviz.persistence.repository.FunctionRepository;
import net.explorviz.persistence.repository.LandscapeRepository;
import net.explorviz.persistence.repository.SpanRepository;
import net.explorviz.persistence.repository.TraceRepository;
import net.explorviz.persistence.util.Pair;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@GrpcService
public class SpanDataServiceImpl implements SpanDataService {

  private static final Logger LOGGER = Logger.getLogger(SpanDataServiceImpl.class);

  @Inject
  private LandscapeRepository landscapeRepository;

  @Inject
  private ApplicationRepository applicationRepository;

  @Inject
  private CommitRepository commitRepository;

  @Inject
  private FileRevisionRepository fileRevisionRepository;

  @Inject
  private FunctionRepository functionRepository;

  @Inject
  private SpanRepository spanRepository;

  @Inject
  private TraceRepository traceRepository;

  @Inject
  private SessionFactory sessionFactory;

  @Blocking
  @Override
  public Uni<Empty> persistSpan(final SpanData spanData) {

    final String[] splitFqn = spanData.getFunctionFqn().split("\\.");
    final String[] splitFileFqn = Arrays.copyOfRange(splitFqn, 0, splitFqn.length - 1);
    final String commitId = spanData.hasCommitId() ? spanData.getCommitId() : null;

    final Session session = sessionFactory.openSession();

    final Span span = new Span(spanData);

    if (!spanData.getParentId().isEmpty()) {
      final Span parentSpan = spanRepository.getOrCreateSpan(session, spanData.getParentId());
      span.setParentSpan(parentSpan);
    }

    final Trace trace = traceRepository.getOrCreateTrace(session, spanData.getTraceId());
    trace.addChildSpan(span);

    final Landscape landscape =
        landscapeRepository.getOrCreateLandscape(session, spanData.getLandscapeTokenId());
    landscape.addTrace(trace);

    final Function function;
    FileRevision fileRevision = null;

    if (commitId != null) {
      final Pair<FileRevision, Function> result =
          resolveSpanWithCommitId(session, spanData, splitFqn, splitFileFqn, landscape);
      fileRevision = result.first();
      function = result.second();
    } else {
      function = functionRepository.findFunction(session, spanData.getApplicationName(), splitFqn,
          spanData.getLandscapeTokenId()).orElse(new Function(splitFqn[splitFqn.length - 1]));
    }
    span.setFunction(function);

    if (fileRevision == null) {
      try {
        fileRevision = resolveFileRevision(session, spanData, landscape, splitFileFqn);
      } catch (final NoSuchElementException | IllegalArgumentException e) {
        if (LOGGER.isEnabled(Level.ERROR)) {
          LOGGER.error("Error while creating file structure for span: " + e);
        }
        return Uni.createFrom()
            .failure(Status.ABORTED.withDescription("Something went wrong!").asRuntimeException());
      }
    }

    fileRevision.addFunction(function);

    session.save(List.of(span, trace, landscape, fileRevision, function));

    return Uni.createFrom().item(Empty.getDefaultInstance());
  }

  private Pair<FileRevision, Function> resolveSpanWithCommitId(final Session session,
      final SpanData spanData, final String[] splitFqn, final String[] splitFileFqn,
      final Landscape landscape) {
    FileRevision fileRevision;
    Function function =
        functionRepository.findFunction(session, spanData.getApplicationName(), splitFqn,
            spanData.getCommitId(), spanData.getLandscapeTokenId()).orElse(null);

    if (function != null) {
      fileRevision = fileRevisionRepository.findFileRevisionFromAppNameAndCommitHashAndPath(session,
          spanData.getApplicationName(), spanData.getCommitId(), splitFileFqn,
          spanData.getLandscapeTokenId()).orElse(null);
    } else {
      fileRevision = fileRevisionRepository.findFileRevisionFromAppNameAndPathWithoutCommit(session,
          spanData.getApplicationName(), splitFileFqn, spanData.getLandscapeTokenId()).orElse(null);

      if (fileRevision == null) {
        fileRevision = resolveFileRevision(session, spanData, landscape, splitFileFqn);
      }

      function = new Function(splitFqn[splitFqn.length - 1]);
    }

    return new Pair<>(fileRevision, function);
  }

  private FileRevision resolveFileRevision(final Session session, final SpanData spanData,
      final Landscape landscape, final String[] splitFileFqn) {
    final FileRevision fileRevision;

    final Application application =
        applicationRepository.findApplicationByNameAndLandscapeToken(session,
                spanData.getApplicationName(), spanData.getLandscapeTokenId())
            .orElse(new Application(spanData.getApplicationName()));

    if (application.getRootDirectory() == null) {
      // Application did not previously exist, build file structure from scratch
      fileRevision =
          fileRevisionRepository.createFileStructureForNewApplicationFromFqn(session, application,
              splitFileFqn);
    } else {
      // Create missing directories and file for existing Application
      fileRevision =
          fileRevisionRepository.createFileStructureForExistingApplicationFromFileFqn(session,
              splitFileFqn, application.getName(), landscape.getTokenId());
    }

    return fileRevision;
  }

}
