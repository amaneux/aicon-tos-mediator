//package com.avlino.common.datacache;
//
//import com.hazelcast.config.Config;
//import com.hazelcast.config.NetworkConfig;
//import com.hazelcast.core.Hazelcast;
//import com.hazelcast.core.HazelcastInstance;
//import com.hazelcast.map.IMap;
//
//public class HazelcastMain {
//
//    public static void main(String[] args) {
//        // Set network join configuration
//        Config config = new Config();
//        NetworkConfig networkConfig = config.getNetworkConfig();
//        networkConfig.getJoin().getMulticastConfig().setEnabled(true);
//        networkConfig.getJoin().getTcpIpConfig().setEnabled(false);
////                .addMember("192.168.11.172")
////                .addMember("127.0.0.1");
//        // Create a Hazelcast instance
//        HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();
//
//        // Obtain a distributed map
//        IMap<String, String> map = hazelcastInstance.getMap("sample-map");
//
//        if (map.size() == 0) {
//            map.put("key1", "value1");
//            map.put("key2", "value2");
//            System.out.println("sample-map put: size=" + map.size() + ", values = " + map.values());
//        } else {
//            HazelcastMain poller = new HazelcastMain();
//            do {
//                poller.publish(map);
//            } while (true);
//        }
//    }
//
//    void publish(IMap<String, String> map) {
//        System.out.println("sample-map size=" + map.size() + ", values = " + map.values());
//        try {
//            Thread.sleep(1000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
