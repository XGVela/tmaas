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

package org.xgvela.cnf.api.topo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.xgvela.cnf.topo.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiNFService {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name = null;

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

    @JsonProperty("extendedAttrs")
    private Map<String, String> extendedAttrs;

    @JsonIgnore
    private Map<String, ApiNFServiceInstance> elem = new HashMap<>();

    public Map<String, ApiNFServiceInstance> getElem() {
        return elem;
    }

    public void setElem(Map<String, ApiNFServiceInstance> elem) {
        this.elem = elem;
    }

    @JsonProperty("nf_service_instances")
    private List<ApiNFServiceInstance> elemSet = new ArrayList<>();

    public List<ApiNFServiceInstance> getElemSet() {
        return elemSet;
    }

    public void setElemSet(List<ApiNFServiceInstance> elemSet) {
        this.elemSet = elemSet;
    }

    public ApiNFService() {
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

    public Map<String, String> getExtendedAttrs() {
        return extendedAttrs;
    }

    public void setExtendedAttrs(Map<String, String> extendedAttrs) {
        this.extendedAttrs = extendedAttrs;
    }
}