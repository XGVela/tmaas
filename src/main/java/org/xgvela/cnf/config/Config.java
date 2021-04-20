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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.xgvela.cnf.util.NatsUtil;
import io.fabric8.zjsonpatch.JsonPatch;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;

public class Config {

	private static final Logger LOG = LogManager.getLogger(Config.class);
	public static ObjectMapper mapper = new ObjectMapper();


	/**
	 * JsonNode holding current configuration properties
	 */
	private static JsonNode node;

	/**
	 * reads dynamic configuration from file and configures tmaas
	 */
	public static void initialize() {

		// unknown fields will not cause failure
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		LOG.info("Initializing tmaas configuration...");
		try {
			node = mapper.readTree(new File("/config/config.json"));
			updateProperties();
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		LOG.info("Finished initializing dynamic configuration");
	}

	/**
	 * applies update patch on configuration properties
	 *
	 * @param json-patch
	 * @return whether patch was successfully applied or not
	 */
	public static boolean applyPatch(JsonNode patch) {
		boolean status = true;
		LOG.info("Updating tmaas configuration, applying patch: \n" + patch.toPrettyString()
				+ "\n<== configuration ==> \n" + node.toPrettyString());
		try {
			node = JsonPatch.apply(patch, node);
			updateProperties();
		} catch (Exception e) {
			LOG.error(e.getMessage());
			status = false;
		}
		return status;
	}

	/**
	 * publishes config update response to CIM over NATS topic "CONFIG"
	 *
	 * @param response
	 * @param status
	 */
	public static void publishResponse(JsonNode response, boolean status) {
		LOG.debug("Config update response for CIM:  " + response.toPrettyString());
		NatsUtil.getConnection().publish("CONFIG", response.toString().getBytes());
	}

	/**
	 * update properties
	 */
	private static void updateProperties() {
		JsonNode config = node.get("config");
		updateLogLevel(config.get("logLevel").asText());
	}

	/**
	 * updates log level
	 *
	 * @param level
	 */
	public static void updateLogLevel(String level) {
		Configurator.setAllLevels("org.xgvela", Level.toLevel(level));
		LOG.info("Log level Changed to: " + level);
	}
}
