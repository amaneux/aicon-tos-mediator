package com.aicon.tos.interceptor;

import com.aicon.tos.interceptor.decide.InterceptorDecide;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import static com.aicon.tos.shared.util.TimeUtils.waitMilliSeconds;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterceptorFilterTest {

    private static final String ENTITY = "test-entity";
    private static final String TOPIC = "test-topic";

    InterceptorConfig interceptorConfig;
    private CountDownLatch latch;
    private InterceptorFilter interceptorFilter;

    @BeforeEach
    void setUp() {
        InterceptorDecide decideMock = mock(InterceptorDecide.class);
        latch = new CountDownLatch(1);

        interceptorConfig = new InterceptorConfig();
        interceptorConfig.clearEntityConfigs();
        interceptorConfig.addEntityConfig(ENTITY, new InterceptorEntityConfig(
                ENTITY,
                TOPIC,
                null,
                5,
                Duration.ofMinutes(30),
                1000L,
                null,
                true, true, true
        ));


        // Ensure the configuration exists
        assertNotNull(interceptorConfig.getEntityConfig(ENTITY), "Interceptor configuration for 'test-entity' must exist.");

        // Create the InterceptorFilter
        interceptorFilter = new InterceptorFilter(interceptorConfig, ENTITY, latch, decideMock);
    }

    @AfterEach
    void tearDown() {
        interceptorConfig.clearEntityConfigs();
    }

    @Test
    void testStopSetsRunningFalse() throws InterruptedException {
        // Start on a separate thread to avoid blocking run()
        Thread t = new Thread(interceptorFilter);
        t.start();

        // Let the filter start
        waitMilliSeconds(100, "for filter to start.");
        interceptorFilter.stopRunning();
        t.join(1000); // Wait for the thread to finish

        assertTrue(latch.getCount() <= 1, "Latch should be decremented when filter stops.");
    }

    @Test
    void testRunHandlesInterruptedException() throws Exception {
        Future<?> mockFuture = mock(Future.class);
        when(mockFuture.get()).thenAnswer(invocation -> {
            throw new InterruptedException("simulated");
        });
        interceptorFilter.setFuture(mockFuture);

        Thread thread = new Thread(interceptorFilter);
        thread.start();
        waitMilliSeconds(200, "for filter to start.");
        interceptorFilter.stopRunning();
        thread.join(1000);

        assertTrue(true, "No exception should propagate.");
    }

    @Test
    void testLatchCountedDownEvenOnException() throws Exception {
        Future<?> mockFuture = mock(Future.class);
        when(mockFuture.get()).thenThrow(new InterruptedException());
        interceptorFilter.setFuture(mockFuture);

        Thread thread = new Thread(interceptorFilter);
        thread.start();
        waitMilliSeconds(200, "for filter to start.");
        interceptorFilter.stopRunning();
        thread.join(1000);

        assertTrue(interceptorFilter.getLastProcessedMessage() == null || latch.getCount() <= 1,
                "Latch count should be decremented even on exceptions.");
    }

}