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

package org.xgvela.cnf.k8s;

import com.fasterxml.jackson.databind.JsonNode;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.config.Config;
import org.xgvela.cnf.kafka.PodDetails;
import org.xgvela.cnf.util.EtcdUtil;
import org.xgvela.cnf.util.TopoManager;
import org.xgvela.cnf.zk.ZKManager;
import org.xgvela.cnf.zk.ZKUtil;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher.Action;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ConstructTree {

    private static final Logger LOG = LogManager.getLogger(ConstructTree.class);
    public static final CountDownLatch kafkaListenerLatch = new CountDownLatch(1);
    public static final String TOPO_ENGINE_LEADER_ELECTION = "/topo_engine/election";

    public static AtomicBoolean treeReadyFlag = new AtomicBoolean();


    @Autowired
    private K8sUtil k8sClient;

    @Autowired
    private TopoManager manager;

    @Autowired
    private EtcdUtil etcd;

    @Autowired
    K8sUtil k8s;


    @PostConstruct
    public void init() throws Exception {

        new Thread(() -> {
            try {
                initProcessing();
            } catch (Exception e) {
                LOG.error(e.getMessage(),e);
            }
        }).start();
    }

    private void initProcessing() throws Exception {
        // mark tree as unready for endpoint
        treeReadyFlag.set(false);

        LOG.debug("init processing");
        manager.createManagedElement();

        Config.initialize();

        // initialize etcd client
        etcd.initClient();

        ZKManager.selectLeader(TOPO_ENGINE_LEADER_ELECTION);

        //Leader should put the path /me then only flow should proceed
        while (!ZKManager.PathExist(ZKUtil.generatePath())){
            Thread.sleep(200);
        }
        kafkaListenerLatch.countDown();
        treeReadyFlag.set(true);
    }

    public void reconstruct() {

        // mark tree as unready for endpoint
        treeReadyFlag.set(false);

        LOG.info("Reconstructing Topology Tree");
        long startTime = System.currentTimeMillis();
        try {
            for (Pod pod : k8sClient.getClient().pods().inAnyNamespace().list().getItems()) {

                if (pod.getMetadata().getAnnotations() != null
                        && pod.getMetadata().getAnnotations().containsKey(Constants.ANNOTATION_TMAAS)) {

                    JsonNode annotations = TopoManager.mapper
                            .readTree(pod.getMetadata().getAnnotations().get(Constants.ANNOTATION_TMAAS));

                    if (!isValid(annotations)) {

                        LOG.debug("Pod named: [" + pod.getMetadata().getName()
                                + "] has TMaaS annotation but does not have all its required values");
                        continue;
                    }

                    String podName = pod.getMetadata().getName();
                    String namespace = pod.getMetadata().getNamespace();

                    if (!annotations.get(Constants.XGVELA_ID).asText().equals(TopoManager.xgvelaId)) {
                        LOG.debug(podName + ", " + namespace + " does not match self xgvelaId");
                        continue;
                    }

                    // tmaas annotations
                    String nfName = annotations.get(Constants.NF_ID).asText();
                    String nfType = annotations.get(Constants.NF_TYPE).asText();
                    String nfServiceName = annotations.get(Constants.NF_SERVICE_ID).asText();
                    String nfServiceType = annotations.get(Constants.NF_SERVICE_TYPE).asText();

                    // create details object, DNs and IDs
                    PodDetails podDetails = new PodDetails(Action.ADDED, podName, namespace, nfName, nfType,
                            nfServiceName, nfServiceType);

                    LOG.debug(podDetails.getAction() + ", NF name : [" + nfName + "]" + " , NF TYPE :[ " + nfType + " ],NF Service name [ " + nfServiceName + "]");

                    String nfDn = manager.getNfDn(podDetails.getNfName());
                    String nfId = manager.getUUID(nfDn);

                    String nfServiceDn = manager.getNfSvcDn(podDetails.getNfName(), podDetails.getNfServiceName());
                    String nfServiceId = manager.getUUID(nfServiceDn);
                    LOG.debug("adding in the map getNfNameByNfIDMap nf : ["+ nfName + "]  :: nfid ["+nfId+"]");
                    LOG.debug("adding in the map  nfs : ["+ nfServiceName + "]  :: nfsid ["+nfServiceId+"]");
                    String nfServiceInstanceDn = manager.getNfSvcInsDn(podDetails.getNfName(),
                            podDetails.getNfServiceName(), podDetails.getPodName());
                    String nfServiceInstanceId = manager.getUUID(nfServiceInstanceDn);

                    // add to tree
                    LOG.debug("Adding to tree: [" + podName + "]" + ", NFname  [ " + podDetails.getNfName() + " ]" + " service :" + podDetails.getNfServiceName());
                    manager.addToTree(podDetails, nfId, nfDn, nfServiceId, nfServiceDn, nfServiceInstanceId,
                            nfServiceInstanceDn);

                } else {
                    LOG.debug("Pod named: [" + pod.getMetadata().getName()
                            + "] does not have required annotations for TMaaS");
                }
            }
        } catch (NullPointerException | IOException | KubernetesClientException e) {
            LOG.error(e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage(), e);
        } finally {
            long endTime = System.currentTimeMillis();
            // ready for REST requests
            treeReadyFlag.set(true);

            LOG.info("Reconstructed Topology Tree in " + (endTime - startTime) + "ms");
        }
    }

    private boolean isValid(JsonNode annotations) {
        return (annotations.has(Constants.NF_ID) && annotations.has(Constants.NF_SERVICE_ID)
                && annotations.has(Constants.NF_TYPE) && annotations.has(Constants.NF_SERVICE_TYPE)
                && annotations.has(Constants.XGVELA_ID));
    }


}
