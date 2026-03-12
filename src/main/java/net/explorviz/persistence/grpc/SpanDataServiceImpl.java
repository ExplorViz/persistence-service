package net.explorviz.persistence.grpc;

import com.google.common.collect.ObjectArrays;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import java.util.List;
import java.util.NoSuchElementException;
import net.explorviz.persistence.ogm.Application;
import net.explorviz.persistence.ogm.Clazz;
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

    try {
      if (spanData.hasClassName()) {
        final Clazz clazz = resolveSpanWithClass(session, spanData, landscape, span);
        session.save(clazz);
      } else {
        final FileRevision fileRevision =
            resolveSpanWithoutClass(session, spanData, landscape, span);
        session.save(fileRevision);
      }
    } catch (final NoSuchElementException | IllegalArgumentException e) {
      if (LOGGER.isEnabled(Level.ERROR)) {
        LOGGER.error("Error while creating file structure for span: " + e);
      }
      return Uni.createFrom()
          .failure(Status.ABORTED.withDescription("Something went wrong!").asRuntimeException());
    }

    session.save(List.of(span, trace, landscape));

    return Uni.createFrom().item(Empty.getDefaultInstance());
  }

  private FileRevision resolveSpanWithoutClass(final Session session, final SpanData spanData,
      final Landscape landscape, final Span span) {
    final String[] splitFilePath = spanData.getFilePath().split("/");
    final String functionName = spanData.getFunctionName();
    final String commitHash = spanData.hasCommitHash() ? spanData.getCommitHash() : null;

    final Function function;
    FileRevision fileRevision = null;

    if (commitHash != null) {
      final Pair<FileRevision, Function> result =
          resolveSpanWithCommitId(session, spanData, functionName, splitFilePath, landscape);
      fileRevision = result.first();
      function = result.second();
    } else {
      function = functionRepository.findFunction(session, spanData.getApplicationName(),
              ObjectArrays.concat(splitFilePath, functionName), spanData.getLandscapeTokenId())
          .orElse(new Function(functionName));
    }
    span.setFunction(function);

    if (fileRevision == null) {
      fileRevision = resolveFileRevision(session, spanData, landscape, splitFilePath);
    }
    fileRevision.addFunction(function);
    session.save(List.of(span, landscape, fileRevision, function));
    return fileRevision;
  }

  private Clazz resolveSpanWithClass(final Session session, final SpanData spanData,
      final Landscape landscape, final Span span) {
    final String[] splitFilePath = spanData.getFilePath().split("/");
    final String functionName = spanData.getFunctionName();
    final String[] splitClassPath =
        spanData.hasClassName() ? spanData.getClassName().split("\\.") : null;
    final String commitHash = spanData.hasCommitHash() ? spanData.getCommitHash() : null;

    final Function function;
    Clazz clazz = null;

    if (commitHash != null) {
      // TODO: This should be a Pair<Clazz, Function>
      final Pair<Clazz, Function> result =
          resolveSpanWithCommitId(session, spanData, functionName, splitFilePath, splitClassPath,
              landscape);
      clazz = result.first();
      function = result.second();
    } else {
      function = functionRepository.findFunction(session, spanData.getApplicationName(),
          ObjectArrays.concat(splitFilePath, functionName), spanData.getLandscapeTokenId(),
          splitClassPath).orElse(new Function(functionName));
    }
    span.setFunction(function);


    if (clazz == null) {
      // TODO: Muss auch erstellt werden
      clazz = resolveClazz();
    }
    clazz.addFunction(function);
    session.save(List.of(span, landscape, clazz, function));
    return Clazz;
  }

  private Pair<FileRevision, Function> resolveSpanWithCommitId(final Session session,
      final SpanData spanData, final String functionName, final String[] splitFilePath,
      final Landscape landscape) {
    FileRevision fileRevision;
    Function function = functionRepository.findFunction(session, spanData.getApplicationName(),
        ObjectArrays.concat(splitFilePath, functionName), spanData.getCommitHash(),
        spanData.getLandscapeTokenId()).orElse(null);

    if (function != null) {
      fileRevision = fileRevisionRepository.findFileRevisionFromAppNameAndCommitHashAndPath(session,
          spanData.getApplicationName(), spanData.getCommitHash(), splitFilePath,
          spanData.getLandscapeTokenId()).orElse(null);
    } else {
      fileRevision = fileRevisionRepository.findFileRevisionFromAppNameAndPathWithoutCommit(session,
              spanData.getApplicationName(), splitFilePath, spanData.getLandscapeTokenId())
          .orElse(null);

      if (fileRevision == null) {
        fileRevision = resolveFileRevision(session, spanData, landscape, splitFilePath);
      }

      function = new Function(functionName);
    }

    return new Pair<>(fileRevision, function);
  }


  /* TODO: Überladung für mit splitClassPath
     Ähnlich wie bei der anderen resolveSpanWithCommitId (die ohne splitClassPath)
     sollen hier die Funktion gesucht werden und wenn nicht da, alles nötige
     (d.h. Dateipfad, File, Klassenpfad, Funktion) angelegt werden
  * */
  private Pair<Clazz, Function> resolveSpanWithCommitId(final Session session,
      final SpanData spanData, final String functionName, final String[] splitFilePath,
      final String[] splitClassPath, final Landscape landscape) {
    FileRevision fileRevision;
    Clazz clazz;
    Function function;
    /* TODO: Wenn Klassen:
        - Suche Funktion anhand von Landscape, AppName, Dateipfad, CommitHash, Klassenpfad, Funktionsnamen
          - Wenn vorhanden:
            - suche Clazz und returne Pair(Clazz, function)
          - Wenn nicht vorhanden:
            - Suche nach Clazz mit gleichem Abhängigkeiten, aber ohne den CommitHash
              - Wenn vorhanden:
                - Nutze die Clazz
              - Wenn nicht vorhanden:
                - Erstelle kompletten alles was fehlt von Dateipfad, FileRevision, Klassenpfad
                  (alles was nicht direkt mit dem Commit oder einer File, die mit einem Commit in
                    Verbindung steht, in Verbindung steht kann reused werden) ohne Beziehung zu
                    einem Commit -> speichere die unterste Clazz
              - Erstelle Funktion
              - Schicke Pair(clazz, function) zurück
       */

    return new Pair<>(clazz, function);
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

  private FileRevision resolveFileRevision(final Session session, final SpanData spanData,
      final Landscape landscape, final String[] splitFileFqn, final String[] splitClassPath) {
    final FileRevision fileRevision;

    final Application application =
        applicationRepository.findApplicationByNameAndLandscapeToken(session,
                spanData.getApplicationName(), spanData.getLandscapeTokenId())
            .orElse(new Application(spanData.getApplicationName()));
    /* TODO:
        - Wenn nicht:
          1. Erstelle fehlende Teile des Dateipfades, gib FileRevision zurück
          2. Erstelle fehlende Teile des Klassenpfades, gib letzte Klasse zurück
          3. Erstelle Funktion
          (Beachten, dass Datei->1.Klasse->*Klassen->Letzte Klasse->Funktion gesetzt ist)
     */


  }

}
