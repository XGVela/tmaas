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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.xgvela.cnf.Constants;

public class PodNetworksStatus {

	@JsonProperty("name")
	private String name = Constants.EMPTY_STRING;

	@JsonProperty("interface")
	private String intf = Constants.EMPTY_STRING;

	@JsonProperty("default")
	private boolean _default = true;

	@JsonProperty("vips")
	private List<String> vips = new ArrayList<>();

	@JsonProperty("ips")
	private List<String> ips = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIntf() {
		return intf;
	}

	public void setIntf(String intf) {
		this.intf = intf;
	}

	public List<String> getVips() {
		return vips;
	}

	public void setVips(List<String> vips) {
		this.vips = vips;
	}

	public List<String> getIps() {
		return ips;
	}

	public void setIps(List<String> ips) {
		this.ips = ips;
	}

	public boolean is_default() {
		return _default;
	}

	public void set_default(boolean _default) {
		this._default = _default;
	}
}
