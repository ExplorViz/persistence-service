package net.explorviz.persistence.api.v3;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.explorviz.persistence.api.v2.model.TimestampDto;
import net.explorviz.persistence.api.v2.model.TraceDto;
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
      @RestQuery final String commit) {

    final Session session = sessionFactory.openSession();

    final long newestTimestamp = Objects.requireNonNullElse(newest, Long.MAX_VALUE);
    final long oldestTimestamp = Objects.requireNonNullElse(oldest, Long.MIN_VALUE);

    final List<TimestampDto> timestamps;

    if (commit != null) {
      timestamps =
          traceRepository.findTimestampsForLandscapeTokenCommitAndTimeRange(
              session, landscapeToken, newestTimestamp, oldestTimestamp, commit, 1_000_000_000L);
    } else {
      timestamps =
          traceRepository.findTimestampsForLandscapeTokenCommitAndTimeRange(
              session, landscapeToken, newestTimestamp, oldestTimestamp, 1_000_000_000L);
    }

    return timestamps;
  }
}
