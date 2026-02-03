package net.explorviz.persistence.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import net.explorviz.persistence.api.model.Timestamp;
import net.explorviz.persistence.repository.TraceRepository;
import org.jboss.resteasy.reactive.RestPath;

@Path("/v2/landscapes/{landscapeToken}/timestamps")
public class TimestampResource {

  @Inject
  TraceRepository traceRepository;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<Timestamp> getTimestamps(@RestPath final String landscapeToken) {
    return traceRepository.getTraceTimestamps(landscapeToken);
  }
}
