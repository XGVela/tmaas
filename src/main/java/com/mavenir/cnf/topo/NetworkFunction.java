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
import org.xgvela.cnf.k8s.K8sUtil;
import org.xgvela.cnf.util.TopoManager;
import org.xgvela.cnf.zk.ZKManager;
import org.xgvela.cnf.zk.ZKUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class NetworkFunction {

    private static final Logger LOG = LogManager.getLogger(NetworkFunction.class);

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("userLabel")
    private String userLabel;

    @JsonProperty("nfType")
    private String nfType;

    @JsonProperty("state")
    private State state = State.NULL;

    @JsonProperty("nfSwVersion")
    private String swVersion = "";

    @JsonProperty("administrativeState")
    private String administrativeState = "UNLOCKED";

    @JsonProperty("operationalState")
    private String operationalState = "ENABLED";

    @JsonProperty("usageState")
    private String usageState = "ACTIVE";

    @JsonProperty("namespace")
    private String namespace = null;

    @JsonIgnore
    private Map<String, NFService> elem = new HashMap<>();

    @JsonProperty("nf_services")
    private Set<String> elemSet = new HashSet<>();

    @JsonProperty("minActiveCount")
    private int minActiveCount = 0;

    @JsonProperty("instantiatedConfActive")
    private int instantiatedConfActive;

    @JsonProperty("instantiatedNotConf")
    private int instantiatedNotConf;

    @JsonProperty("instantiatedConfInactive")
    private int instantiatedConfInactive;

    @JsonProperty("nullCount")
    private int nullCount;

    @JsonProperty("parent")
    private ManagedElement parent = null;


    @JsonProperty("extendedAttrs")
    private Map<String, String> extendedAttrs = new HashMap<>();

    public NetworkFunction() {
    }

    public String getUpgradeVersion(String upgradeVersionKey) {
        return this.getExtendedAttrs().get(upgradeVersionKey);
    }

    public void setUpgradeVersion(String upgradeVersionKey,String upgradeVersionValue) {
        this.getExtendedAttrs().put(upgradeVersionKey,upgradeVersionValue);
    }

    public void removeUpgradeVersion(String upgradeVersionKey){
        this.getExtendedAttrs().remove(upgradeVersionKey);
    }

    public String getUserLabel() {
        return userLabel;
    }

    public void setUserLabel(String userLabel) {
        this.userLabel = userLabel;
    }

    public String getSwVersion() {
        return swVersion;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    public String getNfType() {
        return nfType;
    }

    public void setNfType(String nfType) {
        this.nfType = nfType;
    }

    public String getAdministrativeState() {
        return administrativeState;
    }

    public void setAdministrativeState(String administrativeState) {
        this.administrativeState = administrativeState;
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

    public void setState(State state) {
        this.state = state;
    }

    public boolean has(String id) {
        try {
            byte[] data = ZKManager.getData(id);
            NFService networkService = TopoManager.mapper.readValue(data, NFService.class);
            this.elem.put(id, networkService);
            this.elemSet.add(id);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public NFService get(String id) {
        return this.elem.get(id);
    }

    public Map<String, NFService> getElem() {
        return elem;
    }

    public void setElem(Map<String, NFService> elem) {
        this.elem = elem;
    }

    public void addElem(String key, NFService value) {
        this.elemSet.add(key);
        this.elem.put(key, value);
    }

    public void removeElem(String nfId, String nfsId) throws RuntimeException {
        String zkTopoKey = ZKUtil.generatePath(nfId, nfsId);
        ZKManager.delete(zkTopoKey);
        this.elem.remove(zkTopoKey);
        this.elemSet.remove(zkTopoKey);
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public int getMinActiveCount() {
        return minActiveCount;
    }

    public void setMinActiveCount(int instanceCount) {
        this.minActiveCount = instanceCount;
    }

    public int getInstantiatedConfActive() {
        return instantiatedConfActive;
    }

    public void setInstantiatedConfActive(int instantiatedConfActive) {
        this.instantiatedConfActive = instantiatedConfActive;
    }


    public int getInstantiatedNotConf() {
        return instantiatedNotConf;
    }

    public void setInstantiatedNotConf(int instantiatedNotConf) {
        this.instantiatedNotConf = instantiatedNotConf;
    }

    public ManagedElement getParent() {
        return parent;
    }

    public void setParent(ManagedElement parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    @JsonIgnore
    public List<NFService> getServiceOfNf() throws Exception {
        return getInstances();
    }


    public void update() throws Exception {
        getInstances();

        int instantiatedConfActive = 0, instantiatedNotConf = 0, instantiatedConfInactive = 0, nullCount = 0;
        Iterator<NFService> iterator = this.getElem().values().iterator();

        while (iterator.hasNext()) {
            NFService sd = iterator.next();
            LOG.debug("NFUPDATE [ " + this.getName() + " ]  service name [ " + sd.getName() + " ]" + " state : " + sd.getState().toString());
            switch (sd.getState()) {
                case INSTANTIATED_CONFIGURED_ACTIVE:
                    instantiatedConfActive++;
                    break;
                case INSTANTIATED_NOT_CONFIGURED:
                    instantiatedNotConf++;
                    break;
                case INSTANTIATED_CONFIGURED_INACTIVE:
                    instantiatedConfInactive++;
                    break;
                case NULL:
                    nullCount++;
                default:
                    break;
            }
        }
        this.instantiatedConfActive = instantiatedConfActive;
        this.instantiatedNotConf = instantiatedNotConf;
        this.instantiatedConfInactive = instantiatedConfInactive;
        this.nullCount = nullCount;
        this.updateState();
    }

    private  List<NFService> getInstances() throws Exception {
        List<NFService> nfServiceList = new ArrayList<>();
        List<String> svcs = ZKManager.getChildren(ZKUtil.generatePath(id));
        for (String svc : svcs) {
            String path = ZKUtil.generatePath(id, svc);
            byte[] svcData = ZKManager.getData(path);
            NFService networkService = TopoManager.mapper.readValue(svcData, NFService.class);
            nfServiceList.add(networkService);
            this.elem.put(path, networkService);
            this.elemSet.add(path);
        }
        return nfServiceList;
    }

    private void updateState() throws Exception {
        State stateToUpdate = this.state;
        LOG.debug("NETWORK FUNCTION [" + this.getName() + "] , old state " + this.state.toString() + " with min active count " + this.minActiveCount);
        LOG.debug("NETWORK FUNCTION [" + this.getName() + "] , count for state calculation :: NullCount : "
                + this.nullCount + " , instantiatedNotConf  : " + this.instantiatedNotConf +
                " instantiatedConfActive " + this.instantiatedConfActive + " instantiatedConfInactive : " + this.instantiatedConfInactive);

        for (int i = 0; i < 2; i++) {

            if (this.nullCount == this.minActiveCount) {
                stateToUpdate = State.NULL;
            }
            if (this.instantiatedNotConf > 0) {
                stateToUpdate = State.INSTANTIATED_NOT_CONFIGURED;
            }
            if ((this.instantiatedConfActive + this.instantiatedConfInactive) >= this.minActiveCount
                    && this.instantiatedConfInactive > 0) {

                stateToUpdate = State.INSTANTIATED_CONFIGURED_INACTIVE;
            }
            if (this.instantiatedConfActive >= this.minActiveCount) {
                stateToUpdate = State.INSTANTIATED_CONFIGURED_ACTIVE;
            }
            // if there is state change, double-check instance count, atmost once,
            // do not trigger for RCP
            if (!TopoManager.me.isRcp() && i < 1 && !this.nfType.equals("xgvela") && stateToUpdate != this.state) {
                K8sUtil k8s = new K8sUtil();
                this.minActiveCount = k8s.getNfInstanceCount(this.namespace, this.name, 0, true);
                LOG.debug("NETWORK FUNCTION UPDATE MINACTIVE COUNT " + this.minActiveCount);
            } else {
                LOG.debug("NETWORK FUNCTION UPDATE MINACTIVE COUNT IN BREAK " + this.minActiveCount);
                break;
            }
        }
        LOG.debug("NETWORK FUNC UPDATED STATE : " + stateToUpdate.toString());
        this.state = stateToUpdate;

    }

    public Map<String, String> getExtendedAttrs() {
        return extendedAttrs;
    }

    public void setExtendedAttrs(Map<String, String> extendedAttrs) {
        this.extendedAttrs = extendedAttrs;
    }
}