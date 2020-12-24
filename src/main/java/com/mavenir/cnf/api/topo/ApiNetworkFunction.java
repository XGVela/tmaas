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

public class ApiNetworkFunction {

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

//    @JsonProperty("operations")
//    private ApiOperation operation;

    @JsonProperty("extendedAttrs")
    private Map<String, String> extendedAttrs;

    @JsonIgnore
    private Map<String, ApiNFService> elem = new HashMap<>();

    public Map<String, ApiNFService> getElem() {
        return elem;
    }

    public void setElem(Map<String, ApiNFService> elem) {
        this.elem = elem;
    }

    @JsonProperty("nf_services")
    private List<ApiNFService> elemSet = new ArrayList<>();


    public List<ApiNFService> getElemSet() {
        return elemSet;
    }

    public void setElemSet(List<ApiNFService> elemSet) {
        this.elemSet = elemSet;
    }

    public ApiNetworkFunction() {
    }

//    public ApiOperation getOperation() {
//        return operation;
//    }
//
//    public void setOperation(ApiOperation operation) {
//        this.operation = operation;
//    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getExtendedAttrs() {
        return extendedAttrs;
    }

    public void setExtendedAttrs(Map<String, String> extendedAttrs) {
        this.extendedAttrs = extendedAttrs;
    }
}