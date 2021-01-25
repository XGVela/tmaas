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

import com.fasterxml.jackson.annotation.JsonProperty;
import org.xgvela.cnf.topo.PodNetworksStatus;
import org.xgvela.cnf.topo.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiNFServiceInstance {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name = null;

    @JsonProperty("dnPrefix")
    private String dnPrefix;

    @JsonProperty("userLabel")
    private String userLabel;

    @JsonProperty("state")
    private State state = State.NULL;

    @JsonProperty("haRole")
    private String haRole = null;

    @JsonProperty("msuid")
    private String msUid = null;

    @JsonProperty("network-status")
    private List<PodNetworksStatus> nws = new ArrayList<>();

    @JsonProperty("extendedAttrs")
    private Map<String, String> extendedAttrs;

//    @JsonProperty("nfServiceInstSwVersion")
//    private String swVersion = "";

    public ApiNFServiceInstance() {
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

    public String getHaRole() {
        return haRole;
    }

    public void setHaRole(String haRole) {
        this.haRole = haRole;
    }

    public String getMsUid() {
        return msUid;
    }

    public void setMsUid(String msUid) {
        this.msUid = msUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PodNetworksStatus> getNws() {
        return nws;
    }

    public void setNws(List<PodNetworksStatus> nws) {
        this.nws = nws;
    }

    public Map<String, String> getExtendedAttrs() {
		return extendedAttrs;
	}

	public void setExtendedAttrs(Map<String, String> extendedAttrs) {
		this.extendedAttrs = extendedAttrs;
	}


}
