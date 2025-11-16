package com.aicon.tos.interceptor.newgenproducerconsumer.testcode;

import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingResponseConsumer;

public class AiconDeckingResponseConsumerMain extends TestConsumerBase {

    public static void main(String[] args) {

        AiconDeckingResponseConsumerMain main = new AiconDeckingResponseConsumerMain();
        main.consumer = new AiconDeckingResponseConsumer();
        main.startPollingLoop();
    }
}
