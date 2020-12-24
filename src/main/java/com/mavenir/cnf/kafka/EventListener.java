// Copyright 2020 Mavenir
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.xgvela.cnf.kafka;

import org.xgvela.cnf.Constants;
import org.xgvela.cnf.k8s.ConstructTree;
import org.xgvela.cnf.util.TopoManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EventListener {

    private static final Logger LOG = LogManager.getLogger(EventListener.class);
    public static final String X_CORRELATION_ID = "X-CorrelationId";

    @Autowired
    TopoManager manager;

    @KafkaListener(topics = Constants.KAFKA_TOPIC, containerFactory = "kafkaListenerContainerFactory")
    private void listen(@Payload PodDetails podDetails, @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                        @Header(X_CORRELATION_ID) String messageCorrelationId,@Header(KafkaHeaders.OFFSET) String offset) {

        try {
            LOG.debug("Partition: " + partition + ", KafkaMsg Offset: " + offset + ", CorrelationId: " + messageCorrelationId + ", " + podDetails.toString());
            ConstructTree.kafkaListenerLatch.await();
            manager.updateManagedElement(podDetails);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
