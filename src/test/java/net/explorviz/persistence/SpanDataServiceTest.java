package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;

import net.explorviz.persistence.proto.SpanData;
import net.explorviz.persistence.proto.SpanDataService;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SpanDataServiceTest {
    @GrpcClient
    SpanDataService spanDataService;

    @Test
    void testPersistSpan() {
        Empty reply = spanDataService.persistSpan(SpanData.newBuilder().setId("id1").setStartTime(1).setEndTime(5).build()).await().atMost(Duration.ofSeconds(5));
        assertNotNull(reply);
    }

}
