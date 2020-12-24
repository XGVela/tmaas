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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.xgvela.cnf.util.TopoManager;
import org.xgvela.cnf.zk.ZKManager;
import org.xgvela.cnf.zk.ZKUtil;

import java.util.*;

public class ManagedElement {

    @JsonProperty("id")
    private String id;

    @JsonProperty("dnPrefix")
    private String dnPrefix;

    @JsonProperty("userLabel")
    private String userLabel;

    @JsonProperty("locationName")
    private String locationName;

    @JsonProperty("managedBy")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ManagedBy managedBy = null;

    @JsonProperty("vendorName")
    private String vendorName;

    @JsonProperty("userDefinedState")
    private String userDefinedState = "NULL";

    @JsonProperty("swVersion")
    private String swVersion;

    @JsonProperty("state")
    private State state = State.INSTANTIATED_NOT_CONFIGURED;

    @JsonProperty("instanceCount")
    private int instanceCount;

    @JsonProperty("instantiatedConfActive")
    private int instantiatedConfActive;

    @JsonProperty("instantiatedNotConf")
    private int instantiatedNotConf;

    @JsonProperty("terminated")
    private int terminated;

    @JsonIgnore
    private Map<String, NetworkFunction> elem = new HashMap<>();

    @JsonProperty("network_functions")
    private Set<String> elemSet = new HashSet<>();

    @JsonProperty("rcp")
    private boolean rcp = false;

    public boolean isRcp() {
        return rcp;
    }

    public void setRcp(boolean rcp) {
        this.rcp = rcp;
    }

    public ManagedElement() {
    }

    public String getDnPrefix() {
        return dnPrefix;
    }

    public void setDnPrefix(String dnPrefix) {
        this.dnPrefix = dnPrefix;
    }

    public String getUserLabel() {
        return userLabel;
    }

    public void setUserLabel(String userLabel) {
        this.userLabel = userLabel;
    }

    public ManagedBy getManagedBy() {
        return managedBy;
    }

    public void setManagedBy(ManagedBy managedBy) {
        this.managedBy = managedBy;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getUserDefinedState() {
        return userDefinedState;
    }

    public void setUserDefinedState(String userDefinedState) {
        this.userDefinedState = userDefinedState;
    }

    public String getSwVersion() {
        return swVersion;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
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

    public Map<String, NetworkFunction> getElem() {
        return elem;
    }

    public boolean has(String id) {
        try {
            byte[] data = ZKManager.getData(id);
            NetworkFunction networkFunction = TopoManager.mapper.readValue(data, NetworkFunction.class);
            this.elem.put(id, networkFunction);
            this.elemSet.add(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public NetworkFunction get(String id) {
        return this.elem.get(id);
    }

    public void setElem(Map<String, NetworkFunction> elem) {
        this.elem = elem;
    }

    public void addElem(String key, NetworkFunction value) {
        this.elemSet.add(key);
        this.elem.put(key, value);
    }

    public void removeElem(String nfId) throws RuntimeException {
        String zkTopoKey = ZKUtil.generatePath(nfId);
        ZKManager.delete(zkTopoKey);
        this.elem.remove(zkTopoKey);
        this.elemSet.remove(zkTopoKey);
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
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

    public int getTerminated() {
        return terminated;
    }

    public void setTerminated(int terminated) {
        this.terminated = terminated;
    }

    public void updateManagedElement() {
        int instanceCount = 0, instantiatedConfActive = 0, instantiatedNotConf = 0, terminated = 0;
        Iterator<NetworkFunction> iterator = this.getElem().values().iterator();
        while (iterator.hasNext()) {
            instanceCount++;
            switch (iterator.next().getState()) {
                case INSTANTIATED_CONFIGURED_ACTIVE:
                    instantiatedConfActive++;
                    break;
                case INSTANTIATED_NOT_CONFIGURED:
                    instantiatedNotConf++;
                    break;
                case NULL:
                    break;
                case TERMINATED:
                    terminated++;
                    break;
                default:
                    break;
            }
        }
        this.instanceCount = instanceCount;
        this.instantiatedConfActive = instantiatedConfActive;
        this.instantiatedNotConf = instantiatedNotConf;
        this.terminated = terminated;
        this.state = getManagedElementState();
    }

    private State getManagedElementState() {
        State state = this.state;

        if (this.instanceCount == 0)
            state = State.NULL;
        else if (this.instantiatedConfActive == this.instanceCount)
            state = State.INSTANTIATED_CONFIGURED_ACTIVE;
        else
            state = State.INSTANTIATED_NOT_CONFIGURED;

        return state;
    }
}
