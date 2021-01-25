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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    private static String bootstrapServers = String.valueOf(System.getenv("KAFKA_SVC_FQDN"));
    private static final String groupId = "topo-engine";

    @Autowired
    RebalanceListener consumerRebalanceListener;
    @Autowired
    KafkaListenerEndpointRegistry registry;


    public KafkaListenerEndpointRegistry getRegistry() {
        return registry;
    }

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, new JsonDeserializer<>(PodDetails.class));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public ConsumerFactory<String, PodDetails> consumerFactory() {
        return new DefaultKafkaConsumerFactory<String, PodDetails>(consumerConfigs(), new StringDeserializer(),
                new JsonDeserializer<>(PodDetails.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PodDetails> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PodDetails> factory = new ConcurrentKafkaListenerContainerFactory<String, PodDetails>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setConsumerRebalanceListener(consumerRebalanceListener);
        return factory;
    }
}
