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

package org.xgvela.cnf.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.k8s.ConstructTree;
import org.xgvela.cnf.kafka.KafkaConfiguration;
import org.xgvela.cnf.util.TopoManager;
import org.xgvela.cnf.zk.Leader;
import org.xgvela.cnf.zk.ZKManager;
import org.xgvela.cnf.zk.ZKUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Endpoint {
    private static Logger LOG = LogManager.getLogger(Endpoint.class);

    public static ObjectMapper mapper = new ObjectMapper();

    public static ApiManager apiManager = new ApiManager();

    @Autowired
    private TopoManager manager;
    @Autowired
    ConstructTree constructTree;
    @Autowired
    KafkaConfiguration kafkaConfig;


    @GetMapping(path = "/api/v1/tmaas/topo", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getRoot(@RequestBody(required = false) JsonNode jsonNode) throws JsonProcessingException {
        LOG.debug("/topo endpoint hit, returning Topology");

        String managedElementId = Constants.EMPTY_STRING, networkFunctionId, nFServiceId,
                nFServiceInstanceId = Constants.EMPTY_STRING;
        if (ConstructTree.treeReadyFlag.get()) {
            if (jsonNode != null) {
                if (jsonNode.has("filter")) {
                    if (jsonNode.get("filter").get("type").asText().equals("subtree")) {
                        if (jsonNode.get("filter").has("ManagedElement"))
                            managedElementId = jsonNode.get("filter").get("ManagedElement").get("id").asText();
                        if (managedElementId.equals(ApiManager.apiMe.getId())) {
                            if (jsonNode.get("filter").has("NetworkFunction")) {
                                networkFunctionId = jsonNode.get("filter").get("NetworkFunction").get("id").asText();
                                if (jsonNode.get("filter").has("NFService")) {
                                    nFServiceId = jsonNode.get("filter").get("NFService").get("id").asText();
                                    if (jsonNode.get("filter").has("NFServiceInstance")) {
                                        nFServiceInstanceId = jsonNode.get("filter").get("NFServiceInstance").get("id")
                                                .asText();
                                        return mapper.writeValueAsString(ApiManager.apiMe.getElem().get(networkFunctionId)
                                                .getElem().get(nFServiceId).getElem().get(nFServiceInstanceId));
                                    } else
                                        return mapper.writeValueAsString(ApiManager.apiMe.getElem().get(networkFunctionId)
                                                .getElem().get(nFServiceId));
                                } else
                                    return mapper.writeValueAsString(ApiManager.apiMe.getElem().get(networkFunctionId));
                            } else
                                return mapper.writeValueAsString(ApiManager.apiMe);
                        } else
                            return "Invalid Managed ElementId";
                    } else
                        return "Filter type is not set to subtree";
                } else
                    return "Invalid Json Body";
            } else
                return apiManager.getTree();
        }
        return mapper.writeValueAsString(new EndpointResponse("Either Invalid Element Id or Tree not ready"));
    }


    @PostMapping(value = "/api/v1/_operations/shutdown", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> shutdown() throws Exception {
        LOG.debug("this is shutdown hook to close kafka listeners and zookeeper");
        try {
            kafkaConfig.getRegistry().stop();
            LOG.debug("kafka listener closed");
            Leader.leaderLatch.countDown();
            Thread.sleep(500);
            ZKManager.close();
            LOG.debug("ZKManager close called");
            return ResponseEntity.status(200).body(Endpoint.mapper.writeValueAsString(new EndpointResponse("shutdown proper")));
        } catch (Exception e) {
            LOG.error("either of kaka or zookeeper errored while closing" + e.getMessage(), e);
            return ResponseEntity.status(200).body(Endpoint.mapper.writeValueAsString(new EndpointResponse("shutdown with error")));
        }

    }


    @PostMapping(value = "/api/v1/tmaas/init")
    public ResponseEntity<String> init() throws Exception {
        LOG.info("init method called to wipe out the storage data");
        try {
            ConstructTree.treeReadyFlag.set(false);
            ZKManager.delete("/me");
            ZKManager.delete("/tmaas/functionset");
            //When create managed element is called the cache of already calculated count for nfs is also deleted
            manager.createManagedElement();
            ZKManager.create(ZKUtil.generatePath(), TopoManager.mapper.writeValueAsBytes(TopoManager.me));
            constructTree.reconstruct();
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Endpoint.mapper.writeValueAsString(new EndpointResponse("init called")));
        } catch (Exception e) {
            LOG.error("exception while calling init method " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Endpoint.mapper.writeValueAsString(new EndpointResponse("error while calling init")));
        } finally {
            ConstructTree.treeReadyFlag.set(true);
        }
    }


}
