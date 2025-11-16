package com.aicon.tos.interceptor.newgenproducerconsumer.testcode;

import com.aicon.tos.interceptor.newgenproducerconsumer.mock.AiconDeckingRequestConsumer;

public class AiconDeckingRequestConsumerMain extends TestConsumerBase {

    public static void main(String[] args) {

        AiconDeckingRequestConsumerMain main = new AiconDeckingRequestConsumerMain();
        main.consumer = new AiconDeckingRequestConsumer();
        main.startPollingLoop();
    }
}

