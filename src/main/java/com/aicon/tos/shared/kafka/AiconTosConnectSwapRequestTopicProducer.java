package com.aicon.tos.shared.kafka;

import com.avlino.aicon.ITVJobResequenceRequest.itv_job_resequence_request_key;
import com.avlino.aicon.ITVJobResequenceRequest.itv_job_resequence_request_value;
import com.avlino.aicon.ITVJobResequenceRequest.requests;
import com.avlino.aicon.ITVJobResequenceRequest.swaps;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;

public class AiconTosConnectSwapRequestTopicProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosConnectSwapRequestTopicProducer.class);

    public AiconTosConnectSwapRequestTopicProducer() {
        super(KafkaConfig.aicon_dispatch_itv_job_resequence_request_topic, false);
        ProducerManager.registerProducer(this);
    }

    public void sendAiconTosConnectRequestTopicMessage(String request_id) {

        itv_job_resequence_request_key msgKey = new itv_job_resequence_request_key();
        msgKey.setRequestIdx(request_id);
        msgKey.setApiIdentifier(12);
        msgKey.setTimestamp(Instant.ofEpochMilli(12345678));

        itv_job_resequence_request_value msgValue = new itv_job_resequence_request_value();
        msgValue.setRequestIdx(msgKey.getRequestIdx());
        msgValue.setAPIIdentifier(msgKey.getApiIdentifier());
        msgValue.setTimestamp(msgKey.getTimestamp());
        try {
            swaps swap = swaps.newBuilder()
                    .setITVId("ITV-12345")
                    .setSequenceNo1(1)
                    .setCntrNo1("CNR-0001")
                    .setCntrVisitId1("VISIT-001")
                    .setBayIdxNo1("BAY-001")
                    .setStackIdxNo1("STACK-001")
                    .setTierIdxNo1("TIER-001")
                    .setSequenceNo2(2)
                    .setCntrNo2("CNR-0002")
                    .setCntrVisitId2("VISIT-002")
                    .setBayIdxNo2("BAY-002")
                    .setStackIdxNo2("STACK-002")
                    .setTierIdxNo2("TIER-002")
                    .build();
            requests.Builder request = requests.newBuilder()
                    .setQCId("QC53")
                    .setSeqCount(2)
                    .setSwaps(Collections.singletonList(swap))
                    .setProgramId("interface")
                    .setInternalIdx1("12345678")
                    .setInternalIdx2("12345678")
                    .setUptUserId("DGL");
            msgValue.setRequests(Collections.singletonList(request.build()));

        } catch (IllegalArgumentException e) {
            LOG.error("Invalid request for request_id: {}", request_id);
            throw e;
        }

        sendMessage(msgKey.toString(), msgValue);
    }
}

//{
//        "APIIdentifier": 12,
//        "requestIdx": "REQ_INDEX_444444",
//        "timestamp": 123456,
//        "requests": [
//        {
//        "QCId": "QC53",
//        "SeqCount": 2,
//        "swaps": [
//        {
//        "ITVId": "T224",
//        "SequenceNo1": {
//        "int": 7
//        },
//        "cntrNo1": {
//        "string": "CLHU3456860"
//        },
//        "cntrVisitId1": {
//        "string": "1"
//        },
//        "bayIdxNo1": {
//        "string": "03"
//        },
//        "StackIdxNo1": {
//        "string": "05"
//        },
//        "tierIdxNo1": {
//        "string": "02"
//        },
//        "SequenceNo2": null,
//        "cntrNo2": null,
//        "cntrVisitId2": null,
//        "bayIdxNo2": null,
//        "StackIdxNo2": null,
//        "tierIdxNo2": null
//        },
//        {
//        "ITVId": "T223",
//        "SequenceNo1": {
//        "int": 8
//        },
//        "cntrNo1": {
//        "string": "CLHU3273530"
//        },
//        "cntrVisitId1": {
//        "string": "2"
//        },
//        "bayIdxNo1": {
//        "string": "03"
//        },
//        "StackIdxNo1": {
//        "string": "07"
//        },
//        "tierIdxNo1": {
//        "string": "02"
//        },
//        "SequenceNo2": null,
//        "cntrNo2": null,
//        "cntrVisitId2": null,
//        "bayIdxNo2": null,
//        "StackIdxNo2": null,
//        "tierIdxNo2": null
//        }
//        ],
//        "uptUserId": "DGL",
//        "programId": "interface",
//        "internalIdx1": {
//        "string": "12345678"
//        },
//        "internalIdx2": {
//        "string": "12345678"
//        }
//        }
//        ]
//        }
