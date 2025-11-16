package com.aicon.tos.connect.flows;

import com.aicon.TestConstants;
import com.aicon.tos.connect.cdc.CDCConfig;
import com.aicon.tos.connect.cdc.CDCDataProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CDCControllerTest {

    private CDCController cdcController;

    @BeforeEach
    void setup() {

        FlowManager flowManager = FlowManager.forTestConfig(
                TestConstants.PATH_TO_TEST_CONFIG_FILES + "conf/mediator/cdccontrollertest.xml");

        // Mock ConfigSettings and CDCDataProcessor
        CDCDataProcessor mockCDCProcessor = Mockito.mock(CDCDataProcessor.class);

        // Set up CDCConfig to return an empty collection
        when(mockCDCProcessor.getCdcConfigTable()).thenReturn(Collections.emptyList());

        // Inject mocks into CDCController
        cdcController = new CDCController(flowManager);
    }

    @Test
    void testInitialization() {
        assertNotNull(cdcController, "CDCController should be instantiated.");
        assertTrue(cdcController.report().contains("CDC Controller"));
    }

    @Test
    void testRunMethodStartsSessions() {
        CDCConfig mockConfig = Mockito.mock(CDCConfig.class);
        when(mockConfig.topicName()).thenReturn("TestTopic");
        CDCDataProcessor mockProcessor = Mockito.mock(CDCDataProcessor.class);
        when(mockProcessor.getCdcConfigTable()).thenReturn(Collections.singletonList(mockConfig));

        // Spy to override the behavior of the run method
        CDCController spyController = Mockito.spy(cdcController);

        // Override the run() method to avoid the loop
        doAnswer(invocation -> {
            spyController.runCDCSession("TestTopic");
            return null;
        }).when(spyController).run();

        spyController.run();

        // Verify that the session was started
        assertEquals(1, spyController.getCDCSessions().size(), "Expected one session to be started for TestTopic.");
    }

    @Test
    void testFindDateForGKeyWithNoActiveSession() {
        // Setup mock config but with no active sessions
        CDCConfig mockConfig = mock(CDCConfig.class);
        when(mockConfig.topicName()).thenReturn("non_existent_topic");

        Long result = cdcController.findDateForGKey(mockConfig, "testKey", 5000L, 1000L);

        // Expect null since no session exists
        assertNull(result, "Expected null when no active session exists.");
    }

@Test
void testSessionEndedUpdatesStatsAndRemovesFromSessions() throws Exception {
    // Mock a CDCSession (instead of FlowSession, since CDCSession extends FlowSession)
    CDCSession mockCDCSession = mock(CDCSession.class);

    // Set up the session's state to DONE
    when(mockCDCSession.getState()).thenReturn(FlowSession.SessionState.DONE);

    // Add the mocked session into the controller's sessions map (using reflection to access private field)
    Field sessionsField = CDCController.class.getDeclaredField("sessions");
    sessionsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<CDCSession, Thread> sessions = (Map<CDCSession, Thread>) sessionsField.get(cdcController);
    sessions.put(mockCDCSession, Thread.currentThread());

    // Call sessionEnded with the mockCDCSession (use the same instance)
    cdcController.sessionEnded(mockCDCSession);

    // Access private stats field to verify that it was updated
    Field statsField = CDCController.class.getDeclaredField("stats");
    statsField.setAccessible(true);
    @SuppressWarnings("unchecked")
    Map<FlowSession.SessionState, Long> stats = (Map<FlowSession.SessionState, Long>) statsField.get(cdcController);

    // Assert that DONE state count was incremented
    assertEquals(1, stats.get(FlowSession.SessionState.DONE), "DONE state count should be incremented.");

    // Assert the CDC session was removed from the sessions map
    assertFalse(sessions.containsKey(mockCDCSession), "The session should be removed from the sessions map.");
}

    @Test
    void testControllerStopStopsSessions() {
        // Mock session and thread
        CDCSession mockSession = mock(CDCSession.class);
        Thread mockThread = mock(Thread.class);

        // Inject mock session into the controller
        cdcController.sessions.put(mockSession, mockThread);

        // Simulate 'stopController' method
        cdcController.stopController();

        // Verify that stopSession() was called for the mocked session
        verify(mockSession, times(1)).stopSession();

        // Verify that 'running' is now false
        assertFalse(cdcController.isRunning(), "Controller running should be false after stop.");
    }
}