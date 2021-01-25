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

package org.xgvela.cnf.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.k8s.K8sUtil;
import org.xgvela.cnf.kafka.PodDetails;
import org.xgvela.cnf.topo.*;
import org.xgvela.cnf.zk.ZKManager;
import org.xgvela.cnf.zk.ZKUtil;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class TopoManager {

    private static final Logger LOG = LogManager.getLogger(TopoManager.class);
    public static final String POD_ANNOTATION_NF_VERSION = "xgvela.com/tmaas.nf.nfVersion";
    public static final String UPGRADEVERSION_KEY = "upgradeVersion";
    public static final String POD_ANNOTATION_SVC_VERSION = "svcVersion";
    public static final String NFSI_VERSION_KEY = "svcVersion";

    @Autowired
    Notifier notifier;

    @Autowired
    K8sUtil k8s;

    @Autowired
    EtcdUtil etcd;

    public static Map<String, String> SelfAnnotations = new HashMap<String, String>();
    public static ObjectMapper mapper = new ObjectMapper();
    public static ManagedElement me;
    public static String xgvelaId;

    public void createManagedElement() throws Exception {
        LOG.info("Creating a new root of Topology Tree");

        me = new ManagedElement();

        SelfAnnotations = k8s.getTopoAnnotation();

        LOG.debug("Reading [" + Constants.ANNOTATION_ME + "] annotation");
        JsonNode meDetails = mapper.readTree(SelfAnnotations.get(Constants.ANNOTATION_ME));

        me.setDnPrefix(String.valueOf(System.getenv("DN_PREFIX")));
        me.setLocationName(meDetails.get("locationName").asText());
        me.setSwVersion(meDetails.get("swVersion").asText());

        if (meDetails.has("managedBy")) {
            ManagedBy managedBy = new ManagedBy();

            if (meDetails.get("managedBy").has("mns.address.primary"))
                managedBy.setPrimary(meDetails.get("managedBy").get("mns.address.primary").asText());
            if (meDetails.get("managedBy").has("mns.address.secondary"))
                managedBy.setSecondary(meDetails.get("managedBy").get("mns.address.secondary").asText());

            me.setManagedBy(managedBy);
        }

        LOG.debug("Reading [" + Constants.ANNOTATION_TMAAS + "] annotation");
        JsonNode tmaasDetails = mapper.readTree(SelfAnnotations.get(Constants.ANNOTATION_TMAAS));
        xgvelaId = tmaasDetails.get("xgvelaId").asText();
        me.setVendorName(tmaasDetails.get("vendorId").asText());
        me.setUserLabel(me.getDnPrefix() + ",ManagedElement=me-" + xgvelaId);
        me.setId(getUUID(me.getUserLabel()));

        LOG.info("Managed Element created successfully");
    }

    public ManagedElement getME() {
        return me;
    }

    public String getNfDn(String nfName) {
        return me.getUserLabel() + ",NetworkFunction=" + nfName;
    }

    public String getNfSvcDn(String nfName, String nfServiceName) {
        return getNfDn(nfName) + ",NFService=" + nfServiceName;
    }

    public String getNfSvcInsDn(String nfName, String nfServiceName, String nfServiceInstanceName) {
        return getNfSvcDn(nfName, nfServiceName) + ",NFServiceInstance=" + nfServiceInstanceName;
    }

    public String getUUID(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes()).toString();
    }

    public void updateManagedElement(PodDetails podDetails) throws Exception {
        String nfDn = getNfDn(podDetails.getNfName());
        String nfId = getUUID(nfDn);

        String nfServiceDn = getNfSvcDn(podDetails.getNfName(), podDetails.getNfServiceName());
        String nfServiceId = getUUID(nfServiceDn);


        String nfServiceInstanceDn = getNfSvcInsDn(podDetails.getNfName(), podDetails.getNfServiceName(),
                podDetails.getPodName());
        String nfServiceInstanceId = getUUID(nfServiceInstanceDn);

        LOG.debug(podDetails.getAction() + ", NFServiceInstance DN: [" + nfServiceInstanceDn + "]");

        switch (podDetails.getAction()) {
            case ADDED:
                addToTree(podDetails, nfId, nfDn, nfServiceId, nfServiceDn, nfServiceInstanceId, nfServiceInstanceDn);
                break;
            case MODIFIED:
                updateInTree(nfServiceInstanceId, nfServiceId, nfId, podDetails);
                break;
            case DELETED:
                deleteFromTree(nfServiceInstanceId, nfServiceId, nfId, podDetails);
                break;
            case ERROR:
                break;
            default:
                break;
        }
    }

    public void addToTree(PodDetails podDetails, String nfId, String nfDn, String nfServiceId, String nfServiceDn,
                          String nfServiceInstanceId, String nfServiceInstanceDn) throws Exception {

        if (me.has(ZKUtil.generatePath(nfId)) && me.get(ZKUtil.generatePath(nfId)).has(ZKUtil.generatePath(nfId, nfServiceId)) && me.get(ZKUtil.generatePath(nfId)).get(ZKUtil.generatePath(nfId, nfServiceId)).has(ZKUtil.generatePath(nfId, nfServiceId, nfServiceInstanceId))) {

            // key already present, treat as modified notification
            LOG.info("NF Service Instance: [" + nfServiceInstanceDn + "] already present, redirecting to update method");
            updateInTree(nfServiceInstanceId, nfServiceId, nfId, podDetails);
            return;
        }

        String microservice = podDetails.getNfServiceName();
        String namespace = podDetails.getNamespace();
        String podName = podDetails.getPodName();

        LOG.info("Pod: [" + podName + "], Microservice: [" + microservice + "], Namespace: [" + namespace + "]");

        // get pod, owner kind and pod state
        Pod pod = k8s.getPod(podName, namespace);
        String kind = k8s.getOwnerKind(pod);
        State state = k8s.getNfServiceInstanceState(pod);
        Map<String, String> podAnnotations = pod.getMetadata().getAnnotations();

        if (kind.equals("RobinRole")) {
            LOG.info("################### RCP platform, relying on " + Constants.TMAAS_NFS_MIN_ACTIVE + " annotation");
            me.setRcp(true);
        }

        switch (state) {
            case READY:
            case NOT_READY:
                try {
                    Object owner = k8s.getOwner(microservice, namespace, kind);
                    if (owner != null) {

                        // create nf service instance
                        LOG.info("Creating NF Service Instance ID: [" + nfServiceInstanceId + "]");
                        NFServiceInstance nfServiceInstance = new NFServiceInstance();
                        nfServiceInstance.setId(nfServiceInstanceId);
                        nfServiceInstance.setDnPrefix(me.getDnPrefix());
                        nfServiceInstance.setUserLabel(nfServiceInstanceDn);
                        nfServiceInstance.setName(podName);
                        nfServiceInstance.setNws(k8s.getNetworkStatus(pod));

                        // get pod labels
                        Map<String, String> podLabels = pod.getMetadata().getLabels();

                        // get node labels
                        String nodeName = pod.getSpec().getNodeName();
                        Map<String, String> nodeLabels = k8s.getNodeLabels(nodeName);

                        nfServiceInstance.setExtendedAttrs(k8s.mergeLabels(podLabels, nodeLabels));

                        String haRole = Constants.NA; // default value in case empty or N/A
                        if (podLabels.containsKey(Constants.LABEL_HA_ROLE)
                                && !podLabels.get(Constants.LABEL_HA_ROLE).isEmpty()) {
                            haRole = podLabels.get(Constants.LABEL_HA_ROLE);
                        }
                        nfServiceInstance.setHaRole(haRole);

                        String msUid = Constants.NULL; // default value in case empty or N/A
                        if (podLabels.containsKey(Constants.LABEL_MS_UID)
                                && !podLabels.get(Constants.LABEL_MS_UID).isEmpty()) {
                            msUid = podLabels.get(Constants.LABEL_MS_UID);
                        }
                        nfServiceInstance.setMsUid(msUid);
                        nfServiceInstance.setState(state);
                        nfServiceInstance.setPodDetails(podDetails);

                        NetworkFunction networkFunction;
                        NFService nfService;

                        boolean nfServiceCreated = false, nfCreated = false;

                        if (me.has(ZKUtil.generatePath(nfId))) {

                            // nf exists
                            LOG.debug("NF ID: [" + nfId + "] exists");
                            networkFunction = me.get(ZKUtil.generatePath(nfId));

                            if (networkFunction.has(ZKUtil.generatePath(nfId, nfServiceId))) {

                                // get nf service
                                LOG.debug("NF Service ID: [" + nfServiceId + "] exists");
                                nfService = networkFunction.get(ZKUtil.generatePath(nfId, nfServiceId));
                            } else {

                                // create nf service
                                LOG.debug("NF Service ID: [" + nfServiceId + "] does not exist, creating it");
                                nfService = new NFService();
                                nfServiceCreated = true;
                            }
                        } else {

                            // create nf
                            LOG.debug("NF ID: [" + nfId + "] does not exist, creating it");
                            networkFunction = new NetworkFunction();
                            networkFunction.setId(nfId);
                            networkFunction.setUserLabel(nfDn);
                            networkFunction.setNfType(podDetails.getNfType());
                            networkFunction.setNamespace(namespace);
                            //See if pod annotation has nf version if not go with the usual flow of getting
                            //the value from namespace which sets default value if no version configured
                            networkFunction.setSwVersion(podAnnotations.get(POD_ANNOTATION_NF_VERSION));
                            if (networkFunction.getSwVersion() == null || networkFunction.getSwVersion().isBlank()) {
                                networkFunction.setSwVersion(k8s.getNfSwVersion(namespace));
                            }
                            LOG.debug("NetworkFunction with nfid +["+nfId+"] , is set with SwVersion : "+ networkFunction.getSwVersion());
                            networkFunction.setName(podDetails.getNfName());
                            nfCreated = true;

                            // create nf service
                            LOG.debug("NF Service ID: [" + nfServiceId + "] does not exist, creating it");
                            nfService = new NFService();
                            nfServiceCreated = true;
                        }

                        LOG.debug("Updating NF Service ID: [" + nfServiceId + "]");
                        State nfServiceOldState = nfService.getState();
                        nfService.addElem(ZKUtil.generatePath(nfId, nfServiceId, nfServiceInstanceId), nfServiceInstance);

                        // get annotations/ set instance count/ get k8s uid
                      //  Map<String, String> ownerAnnotations = new HashMap<>();
                        String k8sUid = "XXXX";

                        if (owner instanceof K8sUtil.RobinRole) {
                            LOG.debug("Owner kind is RobinRole");
//                            ownerAnnotations.put("svcVersion",
//                                    podAnnotations.getOrDefault(Constants.ANNOTATION_SVC_VERSION, "v0"));

                        } else if (owner instanceof Deployment) {
                            LOG.debug("Owner kind is Deployment");
                            Deployment deployment = (Deployment) owner;
                            nfService.setMinReadyCount(k8s.getNfServiceInstanceCount(deployment));
                           // ownerAnnotations = deployment.getMetadata().getAnnotations();
                            k8sUid = deployment.getMetadata().getUid();

                        } else if (owner instanceof StatefulSet) {
                            LOG.debug("Owner kind is StatefulSet");
                            StatefulSet statefulSet = (StatefulSet) owner;
                            nfService.setMinReadyCount(k8s.getNfServiceInstanceCount(statefulSet));
                          //  ownerAnnotations = statefulSet.getMetadata().getAnnotations();
                            k8sUid = statefulSet.getMetadata().getUid();

                        } else {
                            LOG.debug("Owner kind is DaemonSet");
                            DaemonSet daemonSet = (DaemonSet) owner;
                            nfService.setMinReadyCount(k8s.getNfServiceInstanceCount(daemonSet));
                           // ownerAnnotations = daemonSet.getMetadata().getAnnotations();
                            k8sUid = daemonSet.getMetadata().getUid();
                        }


                        if (nfServiceCreated) {

                            // set static values
                            nfService.setId(nfServiceId);
                            nfService.setKind(kind);
                            nfService.setNfServiceType(podDetails.getNfServiceType());
                            nfService.setUserLabel(nfServiceDn);
                            nfService.setSwVersion(podAnnotations.getOrDefault(POD_ANNOTATION_SVC_VERSION, "v0"));
                            nfService.setNamespace(namespace);
                            nfService.setK8sUid(k8sUid);
                            nfService.setName(microservice);

                            if (podAnnotations.containsKey(Constants.TMAAS_NFSI_MIN_READY)) {
                                LOG.debug(Constants.TMAAS_NFSI_MIN_READY
                                        + " annotation present on Pod, considering it as minReadyCount for NFService");
                                nfService
                                        .setMinReadyCount(Integer.parseInt(podAnnotations.get(Constants.TMAAS_NFSI_MIN_READY)));
                            }

                            // set HA enbaled flag and other HA relevant fields
                            nfService.setHaEnabled(k8s.isHAEnabled(podAnnotations));
                            if (nfService.isHaEnabled()) {
                                nfService.setNumStandby(k8s.getNumStandby(podAnnotations));
                                nfService.setMode(k8s.getMode(podAnnotations));
                                nfService.setMonitoringMode(k8s.getMonitoringMode(podAnnotations));
                            }
                        }
                        //nfservice instance being added with the svc version
                        nfServiceInstance.setSvcVersion(NFSI_VERSION_KEY, podAnnotations.getOrDefault(POD_ANNOTATION_SVC_VERSION, "v0"));
                        LOG.debug("nfservice instance was set with service version " + nfServiceInstance);
                        ZKManager.updateData(ZKUtil.generatePath(nfId, nfService.getId(), nfServiceInstance.getId()), TopoManager.mapper.writeValueAsBytes(nfServiceInstance));

                        //See if nfs is undergoing upgrade if not, check if upgrade is required
                        if (nfService.getUpgradeVersion(UPGRADEVERSION_KEY) == null) {
                            LOG.debug("Upgrade Version is null checking if it should be upgraded nfId :: " + nfId + ", nfsId :" + nfService.getId());
                            //List<NFServiceInstance> serviceInstanceList = nfService.getNfServiceInstances(nfId);
                            //check if versions are different if yes then set the upgrade version at nf and nfs level
                            String oldVersion = nfService.getSwVersion();
                            LOG.debug("Check nfservice for version upgrade : nfsId " + nfService.getId() + ", current version " + oldVersion);
                           // for (NFServiceInstance nfsi : serviceInstanceList) {
                            LOG.debug("check for version upgrade nfservice for nfsId  [" + nfService.getId() + "]  nfService version : ["+ oldVersion +"] equating with nfsiVersion : [" + nfServiceInstance.getSvcVersion(NFSI_VERSION_KEY) +"]");
                            if (!(nfServiceInstance.getSvcVersion(NFSI_VERSION_KEY).equalsIgnoreCase(oldVersion))) {
                                LOG.debug("Nfservice for nfsId  [" + nfService.getId() + "] marked for upgradation as new version : [" + nfServiceInstance.getSvcVersion(NFSI_VERSION_KEY) + "], and old version [" + oldVersion + "]");
                                nfService.setUpgradeVersion(UPGRADEVERSION_KEY, nfServiceInstance.getSvcVersion(NFSI_VERSION_KEY));
                                //There can be a scenario when two service of same nf can be in upgrade so inorder to not send the notification again
                                //Need to see if upgrade notification for nf to be sent.
                                if (networkFunction.getUpgradeVersion(UPGRADEVERSION_KEY) == null) {
                                    String newNFVersion = podAnnotations.get(POD_ANNOTATION_NF_VERSION);
                                    if (newNFVersion == null) {
                                        newNFVersion = k8s.getNfSwVersion(namespace);
                                    }
                                    LOG.debug("NF for nfId  [" + networkFunction.getId() + "] marked for upgradation as new version  : [" + newNFVersion + "]");

                                    networkFunction.setUpgradeVersion(UPGRADEVERSION_KEY, newNFVersion);
                                    //send the network function going for upgrade notification
                                    notifier.notify(Constants.NF_UPGRADE_PROGRESS, me.getId(), nfId, nfServiceId, nfServiceInstanceId,
                                            nfServiceInstanceId, nfServiceInstanceDn, null, null, podDetails,
                                            networkFunction.getSwVersion(), nfService.getSwVersion(), nfService.isHaEnabled(),
                                            nfServiceInstance.getHaRole(), nfServiceInstance.getMsUid(), null, networkFunction.getExtendedAttrs());
                                }
                                notifier.notify(Constants.NFS_UPGRADE_PROGRESS, me.getId(), nfId, nfServiceId, nfServiceInstanceId,
                                        nfServiceInstanceId, nfServiceInstanceDn, null, null, podDetails,
                                        networkFunction.getSwVersion(), nfService.getSwVersion(), nfService.isHaEnabled(),
                                        nfServiceInstance.getHaRole(), nfServiceInstance.getMsUid(), null, networkFunction.getExtendedAttrs());

                            }

                        }
                        //upgrade logic completed

                        nfService.update(nfId);

                        ZKManager.updateData(ZKUtil.generatePath(nfId, nfService.getId()), TopoManager.mapper.writeValueAsBytes(nfService));

                        // update nf
                        LOG.debug("Updating NF ID: [" + nfId + "]");
                        if (!K8sUtil.IsNfByNamespaceInstanceCountCalculated( podDetails.getNfName(),namespace)) {

                            LOG.info("NetworkFunction was created, setting minActive count for it...");
                            if (podAnnotations.containsKey(Constants.TMAAS_NFS_MIN_ACTIVE)) {
                                LOG.debug(Constants.TMAAS_NFS_MIN_ACTIVE
                                        + " annotation present on Pod, considering it as minActiveCount for NFService");
                                networkFunction.setMinActiveCount(
                                        Integer.parseInt(podAnnotations.get(Constants.TMAAS_NFS_MIN_ACTIVE)));

                            } else {
                                if (me.isRcp()) {
                                    LOG.warn(Constants.TMAAS_NFS_MIN_ACTIVE
                                            + " annotation not present on pod and platform is RCP, setting number of min active services in the NF as 1");
                                    networkFunction.setMinActiveCount(1);
                                } else {
                                    LOG.debug("Non-Robin PaaS, calculating minActiveCount for NF");
                                    networkFunction.setMinActiveCount(k8s.getNfInstanceCount(namespace, podDetails.getNfName(),
                                            networkFunction.getMinActiveCount()));
                                }
                            }
                            //TODO : update network function for the network
                            K8sUtil.addNetworkFunctionCount(podDetails.getNfName(),namespace);
                        }

                        State networkFunctionOldState = networkFunction.getState();
                        networkFunction.addElem(ZKUtil.generatePath(nfId, nfServiceId), nfService);
                        networkFunction.update();
                        ZKManager.updateData(ZKUtil.generatePath(nfId), TopoManager.mapper.writeValueAsBytes(networkFunction));

                        // update managed element
                        me.addElem(ZKUtil.generatePath(nfId), networkFunction);
                        ZKManager.updateData(ZKUtil.generatePath(), TopoManager.mapper.writeValueAsBytes(me));


                        // nf created/modified notification
                        if (nfCreated)
                            notifier.notify(Constants.NF_CREATED, me.getId(), nfId, null, null, nfId, nfDn, null, null,
                                    podDetails, networkFunction.getSwVersion(), null);

                        // state change event raise for NF
                        notifier.notify(Constants.NF_STATE_CHANGED, me.getId(), nfId, null, null, nfId, nfDn,
                                networkFunctionOldState.toString(), networkFunction.getState().toString(), podDetails,
                                networkFunction.getSwVersion(), null);

                        // nf service created/modified notification
                        if (nfServiceCreated)
                            notifier.notify(Constants.NFS_CREATED, me.getId(), nfId, nfServiceId, null, nfServiceId,
                                    nfServiceDn, null, null, podDetails, networkFunction.getSwVersion(),
                                    nfService.getSwVersion());

                        // state change event raise for NFService
                        notifier.notify(Constants.NFS_STATE_CHANGED, me.getId(), nfId, nfServiceId, null, nfServiceId,
                                nfServiceDn, nfServiceOldState.toString(), nfService.getState().toString(), podDetails,
                                networkFunction.getSwVersion(), nfService.getSwVersion());

                        // notification for new NF Service Instance created
                        notifier.notify(Constants.NFSI_CREATED, me.getId(), nfId, nfServiceId, nfServiceInstanceId,
                                nfServiceInstanceId, nfServiceInstanceDn, null, null, podDetails,
                                networkFunction.getSwVersion(), nfService.getSwVersion(), nfService.isHaEnabled(),
                                nfServiceInstance.getHaRole(), nfServiceInstance.getMsUid(), null, nfServiceInstance.getExtendedAttrs());

                        // state change event raise for NFServiceInstance
                        notifier.notify(Constants.NFSI_STATE_CHANGED, me.getId(), nfId, nfServiceId, nfServiceInstanceId,
                                nfServiceInstanceId, nfServiceInstanceDn, State.NULL.toString(),
                                nfServiceInstance.getState().toString(), podDetails, networkFunction.getSwVersion(),
                                nfService.getSwVersion(), nfService.isHaEnabled(), nfServiceInstance.getHaRole(),
                                nfServiceInstance.getMsUid(), Constants.STATE, nfServiceInstance.getExtendedAttrs());

                    } else {
                        LOG.error("Microservice controller (deployment/daemon set/stateful set) not found with name: "
                                + microservice + " in namespace: " + namespace + ", unable to process further");
                    }
                    break;
                } catch (Exception e) {
                    LOG.error("exception while processing add event", e);
                }

            case TERMINATED:
                LOG.debug("State of NF Service Instance is Terminated, removing from tree");
                deleteFromTree(nfServiceInstanceId, nfServiceId, nfId, podDetails);
                break;

            case NULL:
                LOG.debug("State of NF Service Instance is Null, not yet added to tree");
                break;

            default:
                break;
        }
    }

    private void updateInTree(String nfServiceInstanceId, String nfServiceId, String nfId, PodDetails podDetails) throws Exception {

        String microservice = podDetails.getNfServiceName();
        String namespace = podDetails.getNamespace();
        String podName = podDetails.getPodName();

        LOG.info("Pod: [" + podName + "], Microservice: [" + microservice + ", Namespace: [" + namespace + "]");

        // get instance state
        LOG.debug("Updating NF Service Instance ID: [" + nfServiceInstanceId + "]");
        Pod pod = k8s.getPod(podName, namespace);
        State nfServiceInstanceNewState = k8s.getNfServiceInstanceState(pod);
        if (me.has(ZKUtil.generatePath(nfId))) {
            NetworkFunction networkFunc = me.get(ZKUtil.generatePath(nfId));
            if (networkFunc.has(ZKUtil.generatePath(nfId, nfServiceId))) {
                NFService nfService = networkFunc.get(ZKUtil.generatePath(nfId, nfServiceId));
                if (nfService.has(ZKUtil.generatePath(nfId, nfServiceId, nfServiceInstanceId))) {
                    NFServiceInstance nfServiceInst = nfService.get(ZKUtil.generatePath(nfId, nfServiceId, nfServiceInstanceId));
                    if (!nfServiceInstanceNewState.equals(State.NULL)
                            && !nfServiceInstanceNewState.equals(State.TERMINATED)) {

                        // update nf service instance state
                        LOG.debug("Pod exists, updating in tree");
                        State nfServiceInstanceOldState = nfServiceInst.getState();
                        nfServiceInst.setState(nfServiceInstanceNewState);
                        LOG.info("NFService instance state transition Pod: [" + podName + "]" + " oldstate : " + nfServiceInstanceOldState + " new state " + nfServiceInstanceNewState);

                        // get pod labels
                        Map<String, String> podLabels = pod.getMetadata().getLabels();

                        // get node labels
                        String nodeName = pod.getSpec().getNodeName();
                        Map<String, String> nodeLabels = k8s.getNodeLabels(nodeName);

                        //Need to merge nfsi version as k8s mergeLabel will override the value
                        String nfInstSwVersion = nfServiceInst.getSvcVersion(NFSI_VERSION_KEY);

                        nfServiceInst.setExtendedAttrs(k8s.mergeLabels(podLabels, nodeLabels));
                        nfServiceInst.setSvcVersion(NFSI_VERSION_KEY, nfInstSwVersion);

                        // store old HA role
                        String oldHaRole = nfServiceInst.getHaRole();

                        // update nf service instance ha role, store new role
                        String newHaRole = Constants.NA;
                        if (pod.getMetadata().getLabels().containsKey(Constants.LABEL_HA_ROLE)
                                && !pod.getMetadata().getLabels().get(Constants.LABEL_HA_ROLE).isEmpty()) {
                            newHaRole = pod.getMetadata().getLabels().get(Constants.LABEL_HA_ROLE);
                        }
                        nfServiceInst.setHaRole(newHaRole);

                        // store old MS Uid
                        String oldMsUid = nfServiceInst.getMsUid();

                        // update nf service instance msuid, store new MS Uid
                        String newMsUid = Constants.NULL;
                        if (pod.getMetadata().getLabels().containsKey(Constants.LABEL_MS_UID)
                                && !pod.getMetadata().getLabels().get(Constants.LABEL_MS_UID).isEmpty()) {
                            newMsUid = pod.getMetadata().getLabels().get(Constants.LABEL_MS_UID);
                        }
                        nfServiceInst.setMsUid(newMsUid);

                        // update network status
                        nfServiceInst.setNws(k8s.getNetworkStatus(pod));
                        ZKManager.updateData(ZKUtil.generatePath(nfId, nfServiceId, nfServiceInstanceId), TopoManager.mapper.writeValueAsBytes(nfServiceInst));
                        // update nf service state
                        LOG.debug("Updating NF Service ID: [" + nfServiceId + "]");
                        State nfServiceOldState = nfService.getState();
                        nfService.update(nfId);
                        State nfServiceNewState = nfService.getState();
                        ZKManager.updateData(ZKUtil.generatePath(nfId, nfServiceId), TopoManager.mapper.writeValueAsBytes(nfService));

                        // update nf state
                        LOG.debug("Updating NF ID: [" + nfId + "]");
                        State nfOldState = networkFunc.getState();
                        networkFunc.update();
                        State nfNewState = networkFunc.getState();
                        //update the instance ,service,  nf in database
                        ZKManager.updateData(ZKUtil.generatePath(nfId), TopoManager.mapper.writeValueAsBytes(networkFunc));

                        // state change event raise for NF
                        notifier.notify(Constants.NF_STATE_CHANGED, me.getId(), nfId, null, null, nfId,
                                networkFunc.getUserLabel(), nfOldState.toString(), nfNewState.toString(), podDetails,
                                networkFunc.getSwVersion(), null);

                        // state change event raise for NFService
                        notifier.notify(Constants.NFS_STATE_CHANGED, me.getId(), nfId, nfServiceId, null, nfServiceId,
                                nfService.getUserLabel(), nfServiceOldState.toString(),
                                nfServiceNewState.toString(), podDetails, networkFunc.getSwVersion(),
                                nfService.getSwVersion());

                        // state change event raise for NFServiceInstance
                        notifier.notify(Constants.NFSI_STATE_CHANGED, me.getId(), nfId, nfServiceId,
                                nfServiceInstanceId, nfServiceInstanceId,
                                nfServiceInst.getUserLabel(),
                                nfServiceInstanceOldState.toString(), nfServiceInstanceNewState.toString(), podDetails,
                                networkFunc.getSwVersion(), nfService.getSwVersion(),
                                nfService.isHaEnabled(), newHaRole, newMsUid, Constants.STATE, nfServiceInst.getExtendedAttrs());

                        // role change event for NFServiceInstance
                        if (!(oldHaRole.equals(newHaRole) && oldMsUid.equals(newMsUid))) {
                            notifier.notify(Constants.NFSI_STATE_CHANGED, me.getId(), nfId, nfServiceId,
                                    nfServiceInstanceId, nfServiceInstanceId,
                                    nfServiceInst.getUserLabel(), oldHaRole,
                                    newHaRole, podDetails, networkFunc.getSwVersion(),
                                    nfService.getSwVersion(),
                                    nfService.isHaEnabled(), newHaRole, newMsUid,
                                    Constants.HA_ROLE, nfServiceInst.getExtendedAttrs());
                        }
                    } else {

                        // pod does not exist, delete nf service instance from tree
                        LOG.debug("Pod does not exist anymore, invoking deletion of NF Service Instance");
                        deleteFromTree(nfServiceInstanceId, nfServiceId, nfId, podDetails);
                    }
                } else {

                    // nf service instance does not exist in tree
                    LOG.debug("NF Service Instance ID: [" + nfServiceInstanceId + "] does not exist for pod: ["
                            + podName + "]");
                }
            } else {

                // nf service does not exist in tree
                LOG.debug(
                        "NF Service ID: [" + nfServiceId + "] does not exist for microservice: [" + microservice + "]");
            }
        } else {

            // nf does not exist in tree
            LOG.debug("NF ID: [" + nfId + "] does not exist in tree");
        }
    }

    private void deleteFromTree(String nfServiceInstanceId, String nfServiceId, String nfId, PodDetails podDetails) throws Exception {

        String microservice = podDetails.getNfServiceName();
        String namespace = podDetails.getNamespace();
        String podName = podDetails.getPodName();

        LOG.info("Pod: [" + podName + "], Microservice: [" + microservice + "], Namespace: [" + namespace + "]");

        // managed element of this pod instance might not have receive any add event thus updating the managed element
        // this is required so as to have see if this rcp env
        byte[] data = ZKManager.getData(ZKUtil.generatePath());
        me = TopoManager.mapper.readValue(data, ManagedElement.class);
        // tree contains nf
        if (me.has(ZKUtil.generatePath(nfId))) {
            NetworkFunction networkFunction = me.get(ZKUtil.generatePath(nfId));
            // tree contains the nf service
            if (networkFunction.has(ZKUtil.generatePath(nfId, nfServiceId))) {

                NFService srvc = networkFunction.get(ZKUtil.generatePath(nfId, nfServiceId));

                // tree contains the instance, has not already been deleted
                if (srvc.has(ZKUtil.generatePath(nfId, nfServiceId, nfServiceInstanceId))) {

                    State nfServiceOldState, nfServiceNewState = State.NULL, nfOldState, nfNewState,
                            nfServiceInstanceOldState;

                    NFServiceInstance srvcInstance = srvc.get(ZKUtil.generatePath(nfId, nfServiceId, nfServiceInstanceId));

                    nfServiceInstanceOldState = srvcInstance.getState();

                    Map<String, String> extendedAttrs = srvcInstance.getExtendedAttrs();

                    // remove nf service instance from nf service
                    LOG.debug("Deleting NF Service Instance ID: [" + nfServiceInstanceId + "] from NF Service ID: ["
                            + nfServiceId + "] in NF ID: [" + nfId + "]");

                    // store label for notification
                    String nfServiceInstanceUserLabel = srvcInstance.getUserLabel();

                    srvc.removeElem(ZKUtil.generatePath(nfId, nfServiceId, nfServiceInstanceId));

                    nfServiceOldState = srvc.getState();
                    nfOldState = networkFunction.getState();


                    //If service is undergoing upgrade need to see if on removal of srvc instance it needs to mark upgrade complete
                    if (srvc.getUpgradeVersion(UPGRADEVERSION_KEY) != null) {
                        LOG.debug("checking if service needs to be marked completed for upgrade, nfId : " + networkFunction.getId()  +  " nfsId:  "+ srvc.getId() );
                        List<NFServiceInstance> serviceInstanceList = srvc.getNfServiceInstances(nfId);
                        String oldVersion = srvc.getSwVersion();
                        boolean isUpgradeCompleteForNFS = true;
                        //loop through the instance and see if instance with prev version still exists
                        for(NFServiceInstance nfsi : serviceInstanceList){
                            LOG.debug("In delete method : Iterating service instances to check whether oldversion still exist for nfsId  [" + srvc.getId() + "]  nfService version : ["+ oldVersion +"] equating with nfsiVersion : [" + nfsi.getSvcVersion(NFSI_VERSION_KEY) +"]");

                            if ((nfsi.getSvcVersion(NFSI_VERSION_KEY).equalsIgnoreCase(oldVersion))){
                                isUpgradeCompleteForNFS = false;
                                break;
                            }
                        }
                        //If NFS upgrade is complete then need to see if NF upgrade is also completed as at the same time we can
                        //have multiple services running the upgrade
                        if (isUpgradeCompleteForNFS) {

                            LOG.debug("service upgrade completed for  nfId : " + networkFunction.getId()  +  " nfsId:  "+ srvc.getId() + " with new version : "+ srvc.getUpgradeVersion(UPGRADEVERSION_KEY) );
                            srvc.setSwVersion(srvc.getUpgradeVersion(UPGRADEVERSION_KEY));
                            srvc.removeUpgradeVersion(UPGRADEVERSION_KEY);
                            ZKManager.updateData(ZKUtil.generatePath(nfId, nfServiceId), TopoManager.mapper.writeValueAsBytes(srvc));

                            //notify
                            notifier.notify(Constants.NFS_UPGRADE_COMPLETED, me.getId(), nfId, srvc.getId(), null, srvc.getId(), srvc.getUserLabel(),
                                    null, null, podDetails, networkFunction.getSwVersion(), srvc.getSwVersion());

                            //Check if all the services of nf are not in upgrade version then only mark nf upgrade complete
                            List<NFService> nfServicesList =  networkFunction.getServiceOfNf();
                            boolean isUpgradeCompleteForNetworkFunction = true;
                            for (NFService nfServ : nfServicesList) {
                                LOG.debug("In delete method : Iterating services to check whether any service is  still getting upgraded. NfsId  [" + srvc.getId() + "]" + " upgrade version : "+ nfServ.getUpgradeVersion(UPGRADEVERSION_KEY));
                                if (nfServ.getUpgradeVersion(UPGRADEVERSION_KEY) != null) {
                                    isUpgradeCompleteForNetworkFunction = false;
                                }
                            }

                            if (isUpgradeCompleteForNetworkFunction){
                                String nfNewVersion = networkFunction.getUpgradeVersion(UPGRADEVERSION_KEY);
                                LOG.debug("nf upgrade completed for  nfId : " + networkFunction.getId()  +  " nfsId: "+ srvc.getId() + " with new version : "+nfNewVersion);
                                networkFunction.setSwVersion(nfNewVersion);
                                networkFunction.removeUpgradeVersion(UPGRADEVERSION_KEY);
                                notifier.notify(Constants.NF_UPGRADE_COMPLETED, me.getId(), nfId, srvc.getId(), null, networkFunction.getId(), networkFunction.getUserLabel(),
                                        null, null, podDetails, networkFunction.getSwVersion(), srvc.getSwVersion());

                            }

                        }

                    }

                    //upgrade logic completed


                    // update instance count for NFService (in case deployment has scaled down)
                    LOG.debug("Updating NF Service ID: [" + nfServiceId + "]");
                    srvc.update(nfId);
                    ZKManager.updateData(ZKUtil.generatePath(nfId, nfServiceId), TopoManager.mapper.writeValueAsBytes(srvc));
                    nfServiceNewState = srvc.getState();

                    String nfServiceUserLabel = srvc.getUserLabel();
                    String nfServiceSwVersion = srvc.getSwVersion();
                    boolean isHAEnabled = srvc.isHaEnabled();

                    String nfSwVersion = networkFunction.getSwVersion();
                    String nfUserLabel = networkFunction.getUserLabel();

                    //call update so that all the service belonging to the network function are fetched
                    networkFunction.update();

                    Object owner = k8s.getOwner(microservice, namespace, srvc.getKind());

                    // RCP
                    if (me.isRcp()) {

                        if (networkFunction.getElem().entrySet().parallelStream()
                                .noneMatch(entry -> {
                                            try {
                                                NFService entrySrv = entry.getValue();
                                                entrySrv.getNfServiceInstances(nfId);
                                                return entrySrv.getElem().size() != 0;
                                            } catch (Exception e) {
                                                return true;
                                            }
                                        }
                                )
                        ) {

                            networkFunction.getElem().forEach((nfsId, nfs) -> {

                                // remove NF Service from NF
                                LOG.debug("Removing NF Service ID: [" + nfsId + "] in NF ID: [" + nfId + "]");

                                // remove key entries from Etcd in HA-case
                                if (nfs.isHaEnabled()) {
                                    etcd.deletePool(nfs.getNamespace(), nfs.getName(), nfs.getK8sUid());
                                }

                                // deleted notification for NF Service
                                notifier.notify(Constants.NFS_DELETED, me.getId(), nfId, nfsId, null, nfsId,
                                        nfs.getUserLabel(), nfs.getState().toString(), State.TERMINATED.toString(),
                                        podDetails, nfSwVersion, nfs.getSwVersion());

                                // state change event raise for NF Service
                                notifier.notify(Constants.NFS_STATE_CHANGED, me.getId(), nfId, nfsId, null, nfsId,
                                        nfs.getUserLabel(), nfs.getState().toString(), State.TERMINATED.toString(),
                                        podDetails, nfSwVersion, nfs.getSwVersion());
                            });

                            nfNewState = State.TERMINATED;

                            // delete NF from tree
                            LOG.debug("Removing NF ID: [" + nfId + "] (has no NF Service left)");

                            // remove from cache
                            K8sUtil.removeNetworkFunctionCount(networkFunction.getName(),namespace);

                            me.removeElem(nfId);

                            // deleted notification for NF
                            notifier.notify(Constants.NF_DELETED, me.getId(), nfId, null, null, nfId, nfUserLabel,
                                    nfOldState.toString(), nfNewState.toString(), podDetails, nfSwVersion, null);

                        } else {
                            nfNewState = networkFunction.getState();
                            ZKManager.updateData(ZKUtil.generatePath(nfId), TopoManager.mapper.writeValueAsBytes(networkFunction));
                        }

                        // state change event raise for NF
                        notifier.notify(Constants.NF_STATE_CHANGED, me.getId(), nfId, null, null, nfId, nfUserLabel,
                                nfOldState.toString(), nfNewState.toString(), podDetails, nfSwVersion, null);

                    } else {
                        // Non-RCP
                        if (owner != null) {

                            LOG.debug("NFService ID: [" + nfServiceId + "] exists, updating count and state");
                            int instanceCount;
                            if (owner instanceof Deployment) {
                                Deployment deployment = (Deployment) owner;
                                instanceCount = k8s.getNfServiceInstanceCount(deployment);

                            } else if (owner instanceof StatefulSet) {
                                StatefulSet statefulSet = (StatefulSet) owner;
                                instanceCount = k8s.getNfServiceInstanceCount(statefulSet);

                            } else {
                                DaemonSet daemonSet = (DaemonSet) owner;
                                instanceCount = k8s.getNfServiceInstanceCount(daemonSet);
                            }

                            srvc.setMinReadyCount(instanceCount);
                            srvc.update(nfId);
                            ZKManager.updateData(ZKUtil.generatePath(nfId, nfServiceId), TopoManager.mapper.writeValueAsBytes(srvc));
                            nfServiceNewState = srvc.getState();
                            networkFunction.update();
                            nfNewState = networkFunction.getState();
                            ZKManager.updateData(ZKUtil.generatePath(nfId), TopoManager.mapper.writeValueAsBytes(networkFunction));

                        } else {
                            if (srvc.getElem().size() == 0) {

                                LOG.debug("NFService ID: [" + nfServiceId + "] deleted along with all its pods");

                                // remove key entries from Etcd in HA-case
                                if (srvc.isHaEnabled()) {
                                    etcd.deletePool(srvc.getNamespace(),
                                            srvc.getName(),
                                            srvc.getK8sUid());
                                }

                                // remove NF Service from NF
                                LOG.debug("Removing NF Service ID: [" + nfServiceId + "] in NF ID: [" + nfId + "]");
                                networkFunction.removeElem(nfId, nfServiceId);
                                nfServiceNewState = State.TERMINATED;

                            } else {

                                LOG.debug("NFService ID: [" + nfServiceId
                                        + "] deleted but pods still remaining, updating state");
                                srvc.update(nfId);
                                ZKManager.updateData(ZKUtil.generatePath(nfId, nfServiceId), TopoManager.mapper.writeValueAsBytes(srvc));
                                nfServiceNewState = srvc.getState();
                            }

                            // get old state, update NF, get new state
                            LOG.debug("Updating NF ID: [" + nfId + "]");

                            networkFunction.update();
                            ZKManager.updateData(ZKUtil.generatePath(nfId), TopoManager.mapper.writeValueAsBytes(networkFunction));
                            nfNewState = networkFunction.getState();

                            // nf has no nf services left
                            if (networkFunction.getElem().size() == 0) {
                                nfNewState = State.TERMINATED;

                                // remove cim configmaps from namespace
                                k8s.deleteCimConfigMaps(namespace);

                                // remove from cache
                                K8sUtil.removeNetworkFunctionCount(networkFunction.getName(),namespace);

                                // delete NF from tree
                                LOG.debug("Removing NF ID: [" + nfId + "] (has no NF Service left)");
                                me.removeElem(nfId);

                                // deleted notification for NF
                                notifier.notify(Constants.NF_DELETED, me.getId(), nfId, null, null, nfId, nfUserLabel,
                                        nfOldState.toString(), State.TERMINATED.toString(), podDetails, nfSwVersion,
                                        null);
                            }

                            // state change event raise for NF
                            notifier.notify(Constants.NF_STATE_CHANGED, me.getId(), nfId, null, null, nfId, nfUserLabel,
                                    nfOldState.toString(), nfNewState.toString(), podDetails, nfSwVersion, null);
                        }

                        // deleted notification for NF Service
                        if (nfServiceNewState.equals(State.TERMINATED))
                            notifier.notify(Constants.NFS_DELETED, me.getId(), nfId, nfServiceId, null, nfServiceId,
                                    nfServiceUserLabel, nfServiceOldState.toString(), State.TERMINATED.toString(),
                                    podDetails, nfSwVersion, nfServiceSwVersion);

                        // state change event raise for NF Service
                        notifier.notify(Constants.NFS_STATE_CHANGED, me.getId(), nfId, nfServiceId, null, nfServiceId,
                                nfServiceUserLabel, nfServiceOldState.toString(), nfServiceNewState.toString(),
                                podDetails, nfSwVersion, nfServiceSwVersion);
                    }


                    // deleted notification for NF Service Instance
                    notifier.notify(Constants.NFSI_DELETED, me.getId(), nfId, nfServiceId, nfServiceInstanceId,
                            nfServiceInstanceId, nfServiceInstanceUserLabel, nfServiceInstanceOldState.toString(),
                            State.TERMINATED.toString(), podDetails, nfSwVersion, nfServiceSwVersion, isHAEnabled,
                            Constants.NA, Constants.NULL, null, extendedAttrs);

                } else {
                    LOG.debug("Tree does not contain NF Service Instance ID: [" + nfServiceInstanceId + "]");
                }
            } else {
                LOG.debug("Tree does not contain NF Service ID: [" + nfServiceId + "]");
            }
        } else {
            LOG.debug("Tree does not contain NF ID: [" + nfId + "]");
        }
    }
}