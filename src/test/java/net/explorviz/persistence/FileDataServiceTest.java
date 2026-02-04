package net.explorviz.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.protobuf.Timestamp;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import net.explorviz.persistence.ogm.FileRevision;
import net.explorviz.persistence.ogm.Landscape;
import net.explorviz.persistence.proto.CommitData;
import net.explorviz.persistence.proto.CommitService;
import net.explorviz.persistence.proto.FileData;
import net.explorviz.persistence.proto.FileDataService;
import net.explorviz.persistence.proto.FileIdentifier;
import net.explorviz.persistence.proto.Language;
import net.explorviz.persistence.proto.StateDataRequest;
import net.explorviz.persistence.proto.StateDataService;
import org.junit.jupiter.api.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

@QuarkusTest
class FileDataServiceTest {

    private static final long GRPC_AWAIT_SECONDS = 5;

    @GrpcClient
    CommitService commitService;

    @GrpcClient
    StateDataService stateDataService;

    @GrpcClient
    FileDataService fileDataService;

    @Inject
    SessionFactory sessionFactory;

    @Test
    void testAutomaticMappingOfMetric() {
        Session session = sessionFactory.openSession();

        StateDataRequest stateDataRequest = StateDataRequest.newBuilder().setLandscapeToken("mytokenvalue")
                .setRepositoryName("myrepo")
                .setBranchName("main").build();

        stateDataService.getStateData(stateDataRequest).await()
                .atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

        CommitData commitData1 = CommitData.newBuilder().setCommitId("commit1").setRepositoryName("myrepo")
                .setBranchName("main").setLandscapeToken("mytokenvalue")
                .setAuthorDate(Timestamp.newBuilder().setSeconds(1).setNanos(100).build())
                .addAllAddedFiles(List.of(
                        FileIdentifier.newBuilder().setFileHash("1").setFilePath("myrepo/src/File1.java")
                                .build()))
                .build();

        commitService.persistCommit(commitData1).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

        Map<String, Double> testMap = Map.of("count", 1d, "lines", 2d);

        FileData fileData1 = FileData.newBuilder().setLandscapeToken("mytokenvalue").setRepositoryName("myrepo")
                .setFileHash("1").setFilePath("myrepo/src/File1.java").setLanguage(Language.JAVA)
                .addAllImportNames(List.of("Test")).addAllClasses(List.of())
                .addAllFunctions(List.of()).putAllMetrics(testMap)
                .setLastEditor("Testi").setAddedLines(1).setModifiedLines(1).setDeletedLines(0).build();

        fileDataService.persistFile(fileData1).await().atMost(Duration.ofSeconds(GRPC_AWAIT_SECONDS));

        Landscape landscape = session.queryForObject(Landscape.class, """
                MATCH (l:Landscape {tokenId: $tokenId})
                RETURN l;
                """, Map.of("tokenId", "mytokenvalue"));

        FileRevision file = session.queryForObject(FileRevision.class, """
                MATCH (f:FileRevision {hash: $fileHash})
                RETURN f;
                """, Map.of("fileHash", "1"));

        assertEquals(testMap.size(), file.getMetrics().size());
        for (String k : file.getMetrics().keySet()) {
            assertEquals(file.getMetrics().get(k), testMap.get(k));
        }

    }

}
