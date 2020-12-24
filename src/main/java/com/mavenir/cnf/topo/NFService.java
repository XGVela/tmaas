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

package org.xgvela.cnf.topo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.xgvela.cnf.util.TopoManager;
import org.xgvela.cnf.zk.ZKManager;
import org.xgvela.cnf.zk.ZKUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class NFService {
    private static final Logger LOG = LogManager.getLogger(NFService.class);
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("namespace")
    private String namespace = null;

    @JsonProperty("userLabel")
    private String userLabel;

    @JsonProperty("nfServiceType")
    private String nfServiceType;

    @JsonProperty("state")
    private State state = State.NULL;

    @JsonProperty("nfServiceSwVersion")
    private String swVersion = "";

    @JsonProperty("adminstrativeState")
    private String adminstrativeState = "UNLOCKED";

    @JsonProperty("operationalState")
    private String operationalState = "ENABLED";

    @JsonProperty("usageState")
    private String usageState = "ACTIVE";

    @JsonProperty("haEnabled")
    private boolean haEnabled = false;

    @JsonProperty("numStandby")
    private int numStandby = 0;

    @JsonProperty("monitoringMode")
    private String monitoringMode = "";

    @JsonProperty("mode")
    private String mode = "";

    @JsonProperty("kind")
    private String kind = "ReplicaSet";

    @JsonIgnore
    private Map<String, NFServiceInstance> elem = new HashMap<>();

    public String getUpgradeVersion(String upgradeVersionKey) {
        return this.getExtendedAttrs().get(upgradeVersionKey);
    }

    public void setUpgradeVersion(String upgradeVersionKey, String upgradeVersionVal) {
        this.getExtendedAttrs().put(upgradeVersionKey,upgradeVersionVal);
    }

    public void removeUpgradeVersion(String upgradeVersionKey){
        this.getExtendedAttrs().remove(upgradeVersionKey);
    }

    public Set<String> getElemSet() {
        return elemSet;
    }

    public void setElemSet(Set<String> elemSet) {
        this.elemSet = elemSet;
    }

    public int getReadyCount() {
        return readyCount;
    }

    public void setReadyCount(int readyCount) {
        this.readyCount = readyCount;
    }

    public int getNotReadyCount() {
        return notReadyCount;
    }

    public void setNotReadyCount(int notReadyCount) {
        this.notReadyCount = notReadyCount;
    }

    public int getNullCount() {
        return nullCount;
    }

    public void setNullCount(int nullCount) {
        this.nullCount = nullCount;
    }

    public int getActiveReadyCount() {
        return activeReadyCount;
    }

    public void setActiveReadyCount(int activeReadyCount) {
        this.activeReadyCount = activeReadyCount;
    }

    @JsonProperty("nf_service_instances")
    private Set<String> elemSet = new HashSet<>();

    @JsonProperty("k8sUid")
    private String k8sUid = null;

    @JsonProperty("minReadyCount")
    private int minReadyCount;

    @JsonProperty("readyCount")
    private int readyCount;

    @JsonProperty("notReadyCount")
    private int notReadyCount;

    @JsonProperty("nullCount")
    private int nullCount;

    @JsonProperty("activeReadyCount")
    private int activeReadyCount;

    @JsonProperty("parent")
    private NetworkFunction parent = null;

    @JsonProperty("extendedAttrs")
    private Map<String, String> extendedAttrs = new HashMap<>();

    public NFService() {
    }

    public String getSwVersion() {
        return swVersion;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    public int getNumStandby() {
        return numStandby;
    }

    public void setNumStandby(int numStandby) {
        this.numStandby = numStandby;
    }

    public String getUserLabel() {
        return userLabel;
    }

    public void setUserLabel(String userLabel) {
        this.userLabel = userLabel;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isHaEnabled() {
        return haEnabled;
    }

    public void setHaEnabled(boolean haEnabled) {
        this.haEnabled = haEnabled;
    }

    public String getNfServiceType() {
        return nfServiceType;
    }

    public void setNfServiceType(String nfServiceType) {
        this.nfServiceType = nfServiceType;
    }

    public String getAdminstrativeState() {
        return adminstrativeState;
    }

    public void setAdminstrativeState(String adminstrativeState) {
        this.adminstrativeState = adminstrativeState;
    }

    public String getOperationalState() {
        return operationalState;
    }

    public void setOperationalState(String operationalState) {
        this.operationalState = operationalState;
    }

    public String getUsageState() {
        return usageState;
    }

    public void setUsageState(String usageState) {
        this.usageState = usageState;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public State getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getK8sUid() {
        return k8sUid;
    }

    public void setK8sUid(String k8sUid) {
        this.k8sUid = k8sUid;
    }

    public String getMonitoringMode() {
        return monitoringMode;
    }

    public void setMonitoringMode(String monitoringMode) {
        this.monitoringMode = monitoringMode;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Map<String, NFServiceInstance> getElem() {
        return elem;
    }

    public boolean has(String id) {
        if (elemSet.contains(id) && elem.get(id) != null) {
            return true;
        }
        try {
            byte[] data = ZKManager.getData(id);
            NFServiceInstance nwServiceInstance = TopoManager.mapper.readValue(data, NFServiceInstance.class);
            this.elem.put(id, nwServiceInstance);
            this.elemSet.add(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public List<NFServiceInstance> getNfServiceInstances(String nfParent) throws Exception {
        return getInstances(nfParent);
    }

    public NFServiceInstance get(String id) {
        return this.elem.get(id);
    }

    public void setElem(Map<String, NFServiceInstance> elem) {
        this.elem = elem;
    }

    public void addElem(String key, NFServiceInstance value) {
        this.elemSet.add(key);
        this.elem.put(key, value);
    }

    public void removeElem(String key) throws RuntimeException {
        ZKManager.delete(key);
        this.elem.remove(key);
        this.elemSet.remove(key);
    }

    public int getMinReadyCount() {
        return minReadyCount;
    }

    public void setMinReadyCount(int instanceCount) {
        this.minReadyCount = instanceCount;
    }

    public NetworkFunction getParent() {
        return parent;
    }

    public void setParent(NetworkFunction parent) {
        this.parent = parent;
    }

    public void update(String nfParent) throws Exception {
        // Need to bring in all the states of children of this service to calcluate the state.
        getInstances(nfParent);

        int readyCount = 0, notReadyCount = 0, nullCount = 0, activeReadyCount = 0;
        Iterator<NFServiceInstance> iterator = this.getElem().values().iterator();

        while (iterator.hasNext()) {
            NFServiceInstance nfServiceInstance = iterator.next();
            LOG.debug("NF SERVCIE UPDATE  [ " + this.getName() + " ] : ServiceInstance : [ " + nfServiceInstance.getName() + " ]" + " : state : " + nfServiceInstance.getState().toString());
            switch (nfServiceInstance.getState()) {
                case NOT_READY:
                    notReadyCount++;
                    break;
                case READY:
                    readyCount++;
                    if (nfServiceInstance.getHaRole().equals("active"))
                        activeReadyCount++;
                    break;
                case NULL:
                    nullCount++;
                    break;
                default:
                    break;
            }
        }
        this.readyCount = readyCount;
        this.notReadyCount = notReadyCount;
        this.nullCount = nullCount;
        this.activeReadyCount = activeReadyCount;

        this.state = getNFServiceState();
    }

    private List<NFServiceInstance> getInstances(String nfParent) throws Exception {
        List<NFServiceInstance> listOfInstances = new ArrayList<>();
        List<String> svcInstances = ZKManager.getChildren(ZKUtil.generatePath(nfParent, id));
        for (String svcIns : svcInstances) {
            String path = ZKUtil.generatePath(nfParent, id, svcIns);
            byte[] svcInstData = ZKManager.getData(path);
            NFServiceInstance networkServiceInst = TopoManager.mapper.readValue(svcInstData, NFServiceInstance.class);
            listOfInstances.add(networkServiceInst);
            this.elem.put(path, networkServiceInst);
            this.elemSet.add(path);
        }
        return listOfInstances;
    }

    private State getNFServiceState() {
        State state = State.INSTANTIATED_NOT_CONFIGURED;
        LOG.debug("NF SERVICE UPDATE [" + this.name + "] ::  Count for state calculation :: ReadyCount : "
                + this.readyCount + " , MinReadyCount : " + this.minReadyCount + " , NotReadyCount : " + this.notReadyCount +
                " , ActiveReadyCount " + this.activeReadyCount + " , NumStandy : " + this.getNumStandby());

        if (this.readyCount == 0 && this.notReadyCount == 0)
            return State.NULL;

        if (this.readyCount == 0 && this.notReadyCount >= 1)
            return State.INSTANTIATED_NOT_CONFIGURED;

        if (this.haEnabled) {

            if (this.activeReadyCount >= 1)
                state = State.INSTANTIATED_CONFIGURED_INACTIVE;

            if (this.activeReadyCount >= (this.minReadyCount - this.numStandby))
                state = State.INSTANTIATED_CONFIGURED_ACTIVE;
        } else {

            if (this.readyCount >= 1)
                state = State.INSTANTIATED_CONFIGURED_INACTIVE;

            if (this.readyCount >= this.minReadyCount)
                state = State.INSTANTIATED_CONFIGURED_ACTIVE;
        }
        LOG.debug("NETWORK SERVICE [" + this.name + "] :: Updated State : " + state.toString());
        return state;
    }

    public Map<String, String> getExtendedAttrs() {
        return extendedAttrs;
    }

    public void setExtendedAttrs(Map<String, String> extendedAttrs) {
        this.extendedAttrs = extendedAttrs;
    }
}