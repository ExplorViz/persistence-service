package net.explorviz.persistence.api.v3;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.explorviz.persistence.api.v3.model.trace.TimestampDto;
import net.explorviz.persistence.api.v3.model.trace.TraceDto;
import net.explorviz.persistence.ogm.Trace;
import net.explorviz.persistence.repository.TraceRepository;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

/**
 * Contains endpoints concerning dynamic data, i.e. communication data retrieved from runtime
 * analysis of traces.
 */
@Path("/v3/landscapes/{landscapeToken}")
@SuppressWarnings("PMD.UseObjectForClearerAPI")
class TraceResource {

  @Inject SessionFactory sessionFactory;

  @Inject TraceRepository traceRepository;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/dynamic")
  public List<TraceDto> getDynamicData(
      @RestPath final String landscapeToken, @RestQuery final Long from, @RestQuery final Long to) {

    final Session session = sessionFactory.openSession();

    final long fromTimestamp = Objects.requireNonNullElse(from, Long.MIN_VALUE);
    final long toTimestamp = Objects.requireNonNullElse(to, Long.MAX_VALUE);

    final List<Trace> ogmTraces =
        traceRepository.findHydratedTraces(session, landscapeToken, fromTimestamp, toTimestamp);

    return ogmTraces.stream()
        .map(
            t -> {
              final Optional<String> commitHash =
                  traceRepository.findCommitHashForTrace(session, landscapeToken, t.getTraceId());
              return new TraceDto(t, landscapeToken, commitHash.orElse("UNKNOWN"));
            })
        .toList();
  }

  @GET
  @Path("/timestamps")
  @Produces(MediaType.APPLICATION_JSON)
  public List<TimestampDto> getTimestamps(
      @RestPath final String landscapeToken,
      @RestQuery final Long newest,
      @RestQuery final Long oldest,
      @RestQuery final String commit,
      @RestQuery final Long size) {

    final Session session = sessionFactory.openSession();

    final long newestTimestamp = Objects.requireNonNullElse(newest, Long.MAX_VALUE);
    final long oldestTimestamp = Objects.requireNonNullElse(oldest, Long.MIN_VALUE);

    final long defaultBucketSizeNano = 10_000_000_000L; // 10 seconds in nanoseconds

    final long bucketSize;
    if (size == null || size <= 0) {
      bucketSize = defaultBucketSizeNano;
    } else {
      bucketSize = size * 1_000_000L; // Milliseconds to nanoseconds
    }

    final List<TimestampDto> timestamps;

    if (commit != null) {
      timestamps =
          traceRepository
              .findTimestampsForLandscapeTokenAndCommitAndTimeRange(
                  session, landscapeToken, newestTimestamp, oldestTimestamp, commit, bucketSize)
              .stream()
              .map(TimestampDto::new)
              .toList();
    } else {
      timestamps =
          traceRepository
              .findTimestampsForLandscapeTokenAndTimeRange(
                  session, landscapeToken, newestTimestamp, oldestTimestamp, bucketSize)
              .stream()
              .map(TimestampDto::new)
              .toList();
    }

    return timestamps;
  }

  /**
   * Debug function to delete all data gathered from runtime analysis in the landscape.
   *
   * @param landscapeToken String identifier of the visualization landscape
   */
  @DELETE
  @Path("/trace-data")
  public void deleteTraceData(@RestPath final String landscapeToken) {
    final Session session = sessionFactory.openSession();
    traceRepository.deleteTraceData(session, landscapeToken);
  }
}
