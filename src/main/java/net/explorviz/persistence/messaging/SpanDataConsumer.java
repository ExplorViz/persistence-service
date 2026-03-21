package net.explorviz.persistence.messaging;

import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.explorviz.persistence.avro.SpanData;
import net.explorviz.persistence.messaging.service.SpanPersistenceService;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

@ApplicationScoped
public class SpanDataConsumer {

  private static final Logger LOGGER = Logger.getLogger(SpanDataConsumer.class);

  /* default */ @Inject SpanPersistenceService spanPersistenceService;
  /* default */ @Inject SessionFactory sessionFactory;

  @Blocking
  @Incoming("explorviz-spans")
  public void consume(final SpanData spanData) {
    final Session session = sessionFactory.openSession();

    try (Transaction tx = session.beginTransaction()) {
      spanPersistenceService.saveSpanData(session, spanData);
      tx.commit();
    } catch (Exception e) { // NOPMD
      LOGGER.error("Failed to process span: " + spanData.getSpanId(), e);
    }
  }
}
