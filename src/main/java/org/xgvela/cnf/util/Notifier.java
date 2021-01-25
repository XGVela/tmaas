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

package org.xgvela.cnf.util;

import java.util.ArrayList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import org.xgvela.cnf.Constants;
import org.xgvela.cnf.kafka.PodDetails;
import org.xgvela.cnf.notification.KeyValueBean;
import org.xgvela.cnf.notification.NotificationUtil;

@Component
public class Notifier {
	private static final Logger LOG = LogManager.getLogger(Notifier.class);

	public void notify(String notificationName, String meId, String networkFunctionId, String nfServiceId,
			String nfServiceInstanceId, String sourceId, String sourceName, String oldState, String newState,
			PodDetails podDetails, String nfSwVersion, String nfServiceSwVersion) {

		notify(notificationName, meId, networkFunctionId, nfServiceId, nfServiceInstanceId, sourceId, sourceName,
				oldState, newState, podDetails, nfSwVersion, nfServiceSwVersion, false, null, null, null, null);
	}

	public void notify(String notificationName, String meId, String networkFunctionId, String nfServiceId,
			String nfServiceInstanceId, String sourceId, String sourceName, String oldState, String newState,
			PodDetails podDetails, String nfSwVersion, String nfServiceSwVersion, boolean isHaEnabled, String haRole,
			String msUid, String changeType, Map<String, String> extendedAttrs) {

		if ((notificationName.endsWith("StateChanged") && oldState.equalsIgnoreCase(newState))) {
			LOG.info("Suppressing " + notificationName + " for [" + sourceName + "], [" + oldState + "]");
			return;
		}

		try {
			NotificationUtil.sendEvent(notificationName,
					getAdditionalInfo(meId, networkFunctionId, nfServiceId, nfServiceInstanceId, podDetails,
							nfSwVersion, nfServiceSwVersion, isHaEnabled, haRole, msUid, extendedAttrs),
					getStateChangeDef(sourceId, oldState, newState, changeType), sourceId, sourceName);

		} catch (NullPointerException e) {
			LOG.error("Null Pointer Exception occured while raising: " + notificationName + " notification");
			LOG.error(e.getMessage(), e);
		}
	}

	public void scaleNotify(String meId, String nfId, String nfServiceId, String nfServiceInstanceId, String sourceName,
			String podId, String nfName, String nfsName, String status) {

		try {
			NotificationUtil.sendEvent("ScaleInStatus",
					getAdditionalInfo(meId, nfId, nfServiceId, nfServiceInstanceId, podId, nfName, nfsName, status),
					nfServiceInstanceId, sourceName);

		} catch (NullPointerException e) {
			LOG.error("Null Pointer Exception occured while raising: ScaleInStatus notification");
			LOG.error(e.getMessage(), e);
		}
	}

	private ArrayList<KeyValueBean> getAdditionalInfo(String meId, String nfId, String nfsId, String nfsiId,
			String podId, String nfName, String nfsName, String status) {

		ArrayList<KeyValueBean> additionalInfo = new ArrayList<>();

		additionalInfo.add(new KeyValueBean(Constants.ME_ID, meId));
		additionalInfo.add(new KeyValueBean("gracefulShutdownStatus", status));

		additionalInfo.add(new KeyValueBean("nfName", nfName));
		additionalInfo.add(new KeyValueBean(Constants.NF_ID, nfId));

		additionalInfo.add(new KeyValueBean("nfServiceName", nfsName));
		additionalInfo.add(new KeyValueBean(Constants.NF_SERVICE_ID, nfsId));

		additionalInfo.add(new KeyValueBean("nfServiceInstanceName", podId));
		additionalInfo.add(new KeyValueBean(Constants.NF_SERVICE_INSTANCE_ID, nfsiId));

		return additionalInfo;
	}

	public ArrayList<KeyValueBean> getAdditionalInfo(String meId, String networkFunctionId, String nfServiceId,
			String nfServiceInstanceId, PodDetails podDetails, String nfSwVersion, String nfServiceSwVersion,
			boolean isHAEnabled, String haRole, String msUid, Map<String, String> extendedAttrs) {

		ArrayList<KeyValueBean> additionalInfo = new ArrayList<>();

		String nfLabel = TopoManager.me.getUserLabel() + ",NetworkFunction=" + podDetails.getNfName();
		String nfServiceLabel = nfLabel + ",NFService=" + podDetails.getNfServiceName();
		String nfServiceInstanceLabel = nfServiceLabel + ",NFServiceInstance=" + podDetails.getPodName();

		// me fields
		additionalInfo.add(new KeyValueBean(Constants.ME_ID, meId));
		additionalInfo.add(new KeyValueBean("meLabel", TopoManager.me.getUserLabel()));

		// nf fields
		additionalInfo.add(new KeyValueBean(Constants.NF_ID, networkFunctionId));
		additionalInfo.add(new KeyValueBean("nfLabel", nfLabel));
		additionalInfo.add(new KeyValueBean("nfSwVersion", nfSwVersion));

		// nf service fields
		if (nfServiceId != null) {
			additionalInfo.add(new KeyValueBean(Constants.NF_SERVICE_ID, nfServiceId));
			additionalInfo.add(new KeyValueBean("nfServiceLabel", nfServiceLabel));
			additionalInfo.add(new KeyValueBean("nfServiceSwVersion", nfServiceSwVersion));

			// nf service instance fields
			if (nfServiceInstanceId != null) {
				additionalInfo.add(new KeyValueBean(Constants.NF_SERVICE_INSTANCE_ID, nfServiceInstanceId));
				additionalInfo.add(new KeyValueBean("nfServiceInstanceLabel", nfServiceInstanceLabel));
				LOG.debug("Label: [" + nfServiceInstanceLabel + "]");

				// if HA is enabled, include haRole
				if (isHAEnabled && haRole != null) {
					LOG.debug("HA-Role: [" + haRole + "]");
					additionalInfo.add(new KeyValueBean(Constants.LABEL_HA_ROLE, haRole));
				}

				// include msuid regardless of HA
				if (msUid != null) {
					LOG.debug("MS-Uid: [" + msUid + "]");
					additionalInfo.add(new KeyValueBean(Constants.LABEL_MS_UID, msUid));
				}
			}
		}

		if (extendedAttrs != null) {
			LOG.debug("Adding extended attributes for NFServiceInstance");
			extendedAttrs.forEach((attrKey, attrValue) -> {
				additionalInfo.add(new KeyValueBean(attrKey, attrValue));
			});
		}

		// TMaaS annotation values
		additionalInfo.add(new KeyValueBean("nfName", podDetails.getNfName()));
		additionalInfo.add(new KeyValueBean("nfType", podDetails.getNfType()));
		additionalInfo.add(new KeyValueBean("nfServiceName", podDetails.getNfServiceName()));
		additionalInfo.add(new KeyValueBean("nfServiceType", podDetails.getNfServiceType()));
		additionalInfo.add(new KeyValueBean("nfServiceInstanceName", podDetails.getPodName()));

		return additionalInfo;
	}

	public ArrayList<KeyValueBean> getStateChangeDef(String changeIdentifier, String oldState, String newState,
			String changeType) {

		ArrayList<KeyValueBean> stateChangeDef = new ArrayList<>();
		stateChangeDef.add(new KeyValueBean(Constants.CHANGE_IDENTIFIER, changeIdentifier));
		if (changeType != null)
			stateChangeDef.add(new KeyValueBean(Constants.CHANGE_TYPE, changeType));

		if (oldState != null && newState != null) {
			LOG.debug("[" + oldState.toString() + ", " + newState.toString() + "]");
			stateChangeDef.add(new KeyValueBean(Constants.OLD_STATE, oldState));
			stateChangeDef.add(new KeyValueBean(Constants.NEW_STATE, newState));
		}
		return stateChangeDef;
	}
}
