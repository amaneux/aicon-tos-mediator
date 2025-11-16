package com.aicon.tos.shared.kafka;

import com.avlino.aicon.ITVJobResequenceRequest.itv_job_resequence_request_value;
import com.avlino.aicon.ITVJobResequenceRequest.requests;
import com.avlino.aicon.ITVJobResequenceRequest.swaps;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public class ConsumeRequestTopic {

    private static AiconTosConnectSwapRequestTopicConsumer aiconTosConnectSwapRequestTopicConsumer;
    private static final boolean running = true;

    public static void main(String[] args) {

        ConsumerRecords<String, itv_job_resequence_request_value> records = null;

        aiconTosConnectSwapRequestTopicConsumer = new AiconTosConnectSwapRequestTopicConsumer();

        while (running) {
            records = aiconTosConnectSwapRequestTopicConsumer.pollMessages();

            if (records.count() > 0) {
                for (ConsumerRecord<String, itv_job_resequence_request_value> record : records) {
                    itv_job_resequence_request_value value = record.value();

                    if (value != null) {
                        System.out.println("Record ontvangen:");
                        System.out.println("RequestIdx: " + value.getRequestIdx());
                        System.out.println("APIIdentifier: " + value.getAPIIdentifier());
                        System.out.println("Timestamp: " + value.getTimestamp());
                        System.out.println("Requests:");
                        for (requests req : value.getRequests()) {
                            System.out.println("\tQCId: " + req.getQCId());
                            System.out.println("\tSeqCount: " + req.getSeqCount());
                            System.out.println("\tProgramId: " + req.getProgramId());
                            System.out.println("\tInternalIdx1: " + req.getInternalIdx1());
                            System.out.println("\tInternalIdx2: " + req.getInternalIdx2());
                            System.out.println("\tUptUserId: " + req.getUptUserId());
                            System.out.println("\tSwaps:");
                            for (swaps swap : req.getSwaps()) {
                                System.out.println("\t\tITVId: " + swap.getITVId());
                                System.out.println("\t\tSequenceNo1: " + swap.getSequenceNo1());
                                System.out.println("\t\tCntrNo1: " + swap.getCntrNo1());
                                System.out.println("\t\tCntrVisitId1: " + swap.getCntrVisitId1());
                                System.out.println("\t\tBayIdxNo1: " + swap.getBayIdxNo1());
                                System.out.println("\t\tStackIdxNo1: " + swap.getStackIdxNo1());
                                System.out.println("\t\tTierIdxNo1: " + swap.getTierIdxNo1());
                                System.out.println("\t\tSequenceNo2: " + swap.getSequenceNo2());
                                System.out.println("\t\tCntrNo2: " + swap.getCntrNo2());
                                System.out.println("\t\tCntrVisitId2: " + swap.getCntrVisitId2());
                                System.out.println("\t\tBayIdxNo2: " + swap.getBayIdxNo2());
                                System.out.println("\t\tStackIdxNo2: " + swap.getStackIdxNo2());
                                System.out.println("\t\tTierIdxNo2: " + swap.getTierIdxNo2());
                            }
                        }
                    } else {
                        System.out.println("Empty record received.");
                    }
                }
            }
        }
    }
}
