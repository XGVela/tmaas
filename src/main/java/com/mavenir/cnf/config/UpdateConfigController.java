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

package org.xgvela.cnf.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
public class UpdateConfigController {

	private static final Logger LOG = LogManager.getLogger(UpdateConfigController.class);

	@PostMapping(path = "/updateConfig")
	@ResponseStatus(HttpStatus.OK)
	public @ResponseBody JsonNode updateConfig(@RequestBody JsonNode requestBody) throws JsonProcessingException {
		LOG.info("===> Received request for config-update: " + requestBody.toPrettyString());

		UpdateRequest updateRequest = Config.mapper.treeToValue(requestBody, UpdateRequest.class);
		boolean status = Config.applyPatch(Config.mapper.readTree(updateRequest.patch));

		ObjectNode cimResponse = JsonNodeFactory.instance.objectNode();
		cimResponse.put("change-set-key", updateRequest.changeSet);
		cimResponse.put("revision", updateRequest.revision);

		// default: success
		cimResponse.put("status", "success");
		cimResponse.put("remarks", "successfully applied the config update");

		// failure case
		if (!status) {
			LOG.debug("Config update failed");
			cimResponse.put("status", "failure");
			cimResponse.put("remarks", "failed to apply the config update");
		}

		// publish event to CIM over NATS
		Config.publishResponse(cimResponse, status);

		LOG.info("<=== Configuration upto date");
		return cimResponse;
	}
}
