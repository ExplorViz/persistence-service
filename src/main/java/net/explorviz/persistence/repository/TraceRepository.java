package net.explorviz.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Map;
import net.explorviz.persistence.ogm.Trace;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@ApplicationScoped
public class TraceRepository {

  @Inject
  private SessionFactory sessionFactory;

  public Trace findTraceById(final Session session, final String id) {
    return session.queryForObject(Trace.class, "MATCH (t:Trace {traceId: $traceId}) RETURN t;",
        Map.of("traceId", id));
  }

  public Trace findTraceById(final String id) {
    final Session session = sessionFactory.openSession();
    return findTraceById(session, id);
  }
}
