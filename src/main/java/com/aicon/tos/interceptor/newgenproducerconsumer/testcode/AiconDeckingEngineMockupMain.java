package com.aicon.tos.interceptor.newgenproducerconsumer.testcode;

import com.aicon.tos.interceptor.newgenproducerconsumer.mock.AiconDeckingEngineMockup;

public class AiconDeckingEngineMockupMain extends TestConsumerBase {
    public static void main(String[] args) {
        AiconDeckingEngineMockupMain main = new AiconDeckingEngineMockupMain();
        main.consumer = new AiconDeckingEngineMockup();
        main.runInThread();
    }

    private void runInThread() {
        Thread loopThread = new Thread(this::startPollingLoop);
        loopThread.setName("DeckingEngineMockup-PollThread");
        loopThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            consumer.stop(); // break loop
            try {
                loopThread.join(); // wait for poll loop
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            consumer.close(); // consumer clean shutdown
        }));
    }
}
