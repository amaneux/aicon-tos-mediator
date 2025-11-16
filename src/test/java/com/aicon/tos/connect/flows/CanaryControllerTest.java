package com.aicon.tos.connect.flows;

import com.aicon.TestConstants;
import com.aicon.tos.shared.kafka.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CanaryControllerTest {

    private static final Logger log = LoggerFactory.getLogger(CanaryControllerTest.class);
    private CanaryController canaryController;
    private AdminClient adminClient;

    @BeforeEach
    void setup() {
        FlowManager flowManager = FlowManager.forTestConfig(
                TestConstants.PATH_TO_TEST_CONFIG_FILES + "conf/mediator/canarycontrollertest.xml");

        adminClient = AdminClient.create(KafkaConfig.getProducerProps());

        canaryController = new CanaryController(flowManager);
    }


    @AfterEach
    void tearDown() {
        ListTopicsResult topics = adminClient.listTopics();
        Set<String> topicNames = null;
        try {
            topicNames = topics.names().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        // Filter alleen de topics die je zelf hebt aangemaakt
        Set<String> myTopics = topicNames.stream()
                .filter(name -> name.startsWith("tos.test"))
                .collect(Collectors.toSet());
        if (!myTopics.isEmpty()) {
            DeleteTopicsResult deleteResult = adminClient.deleteTopics(myTopics);
            try {
                deleteResult.all().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        canaryController.stopController();

    }

    @Test
    void testGetLastSessionMsg_WhenNoSessions_ShouldReturnDefaultMessage() {


        // Act
        String lastSessionMsg = canaryController.getLastSessionMsg();

        // Assert
        assertEquals("No sessions yet.", lastSessionMsg);
    }

    @Test
    void testGetLastSessionMsg_WithLastSession_ShouldReturnCorrectMessage() throws Exception {
        // Arrange
        FlowSession mockSession = mock(FlowSession.class);
        FlowSessionManager mockSessionManager = mock(FlowSessionManager.class);

        // Set up a mocked session with relevant data
        Date mockDate = new Date();
        when(mockSessionManager.peekLatest()).thenReturn(mockSession);
        when(mockSession.getSessionFinishedTs()).thenReturn(mockDate);
        when(mockSession.getState()).thenReturn(FlowSession.SessionState.DONE); // Ensure state is correctly mocked
        when(mockSession.getMessage()).thenReturn("Session completed without issues.");

        // Verify the mock setup step
        System.out.println("Configured mock session state: " + mockSession.getState());
        assertNotNull(mockSession.getState(), "Mocked session state should not be null.");
        assertEquals(FlowSession.SessionState.DONE, mockSession.getState(),
                "Mocked session state should be DONE.");

        // Use Reflection to replace the private sessionHistoryManager field
        Field sessionManagerField = CanaryController.class.getDeclaredField("sessionHistoryManager");
        sessionManagerField.setAccessible(true);
        sessionManagerField.set(canaryController, mockSessionManager);

        // Act
        String lastSessionMsg = canaryController.getLastSessionMsg();

        // Debugging output
        System.out.println("Retrieved last session message: " + lastSessionMsg);

        // Assert
        assertTrue(lastSessionMsg.contains("Last session finished"),
                "The result should mention the session finished time.");
        assertTrue(lastSessionMsg.contains("State=DONE"),
                "The result should correctly include the session state.");
        assertTrue(lastSessionMsg.contains("Session completed without issues."),
                "The result should include the session's completion message.");
    }

    @Test
    void testStopController_ShouldStopRunningThread() {

        // Act
        canaryController.stopController();

        // Assert
        assertDoesNotThrow(() -> {
            Thread thread = new Thread(canaryController);
            thread.start();
            canaryController.stopController();
            thread.interrupt();
        });
    }
}