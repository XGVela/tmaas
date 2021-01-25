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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.xgvela.cnf.topo.ManagedBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiManagedElement {

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

    @JsonIgnore
    private Map<String, ApiNetworkFunction> elem = new HashMap<>();

    public Map<String, ApiNetworkFunction> getElem() {
        return elem;
    }

    public void setElem(Map<String, ApiNetworkFunction> elem) {
        this.elem = elem;
    }

    public List<ApiNetworkFunction> getElemList() {
        return elemList;
    }

    public void setElemList(List<ApiNetworkFunction> elemList) {
        this.elemList = elemList;
    }


    @JsonProperty("network_functions")
    private List<ApiNetworkFunction> elemList = new ArrayList<>();

    public ApiManagedElement() {
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


}
