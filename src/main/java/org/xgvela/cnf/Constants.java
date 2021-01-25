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

package org.xgvela.cnf;

public class Constants {

	public static final String KAFKA_TOPIC = "TMAAS";
	public static final String KAFKA_LISTENER_ID = "topo.listener";

	public static final String LABEL_MICROSVC = "microSvcName";
	public static final String LABEL_HA_ROLE = "haRole";
	public static final String LABEL_MS_UID = "msuid";

	public static final String NA = "N/A";
	public static final String NULL = "NULL";
	public static final String STARTUP_KEY = "gw-startup-0001";

	public static final String EMPTY_STRING = "";
	public static final String ACTIVITY = "ACTIVITY_TMaaS_";
	public static final String EVENT = "Notification: ";
	public static final String CHANGE_IDENTIFIER = "changeIdentifier";
	public static final String OLD_STATE = "oldState";
	public static final String NEW_STATE = "newState";

	public static final String NFSI_CREATED = "NFServiceInstanceCreated";
	public static final String NFSI_STATE_CHANGED = "NFServiceInstanceStateChanged";
	public static final String NFSI_MODIFIED = "NFServiceInstanceModified";
	public static final String NFSI_DELETED = "NFServiceInstanceDeleted";

	public static final String NFS_CREATED = "NFServiceCreated";
	public static final String NFS_MODIFIED = "NFServiceModified";
	public static final String NFS_STATE_CHANGED = "NFServiceStateChanged";
	public static final String NFS_DELETED = "NFServiceDeleted";

	public static final String NF_CREATED = "NetworkFunctionCreated";
	public static final String NF_MODIFIED = "NetworkFunctionModified";
	public static final String NF_STATE_CHANGED = "NetworkFunctionStateChanged";
	public static final String NF_DELETED = "NetworkFunctionDeleted";

	public static final String NF_UPGRADE_PROGRESS = "NetworkFunctionUpgradeStarted";
	public static final String NF_UPGRADE_COMPLETED = "NetworkFunctionUpgradeCompleted";
	public static final String NFS_UPGRADE_PROGRESS = "NFServiceUpgradeStarted";
	public static final String NFS_UPGRADE_COMPLETED = "NFServiceUpgradeCompleted";

	public static final String ME_ID = "meId";
	public static final String NF_ID = "nfId";
	public static final String NF_TYPE = "nfType";
	public static final String NF_SERVICE_ID = "nfServiceId";
	public static final String NF_SERVICE_TYPE = "nfServiceType";
	public static final String NF_SERVICE_INSTANCE_ID = "nfServiceInstanceId";
	public static final String DN_PREFIX = "dnPrefix";
	public static final String XGVELA_ID = "xgvelaId";

	public static final String ANNOTATION_TMAAS = "xgvela.com/tmaas";
	public static final String ANNOTATION_ME = "xgvela.com/me";
	public static final String ANNOTATION_HA = "xgvela.com/haaas";
	public static final String ANNOTATION_K8S_NWS_STATUS = "k8s.v1.cni.cncf.io/networks-status";
	public static final String ANNOTATION_VIP_STATUS = "xgvela.com/vip-networks-status";
	public static final String ANNOTATION_SVC_VERSION = "svcVersion";

	public static final String TMAAS_NFS_MIN_ACTIVE = "xgvela.com/tmaas.nf.nfs.count";
	public static final String TMAAS_NFSI_MIN_READY = "xgvela.com/tmaas.nf.nfs.nfsi.minReady";

	public static final String STATE = "state";
	public static final String HA_ROLE = "haRole";
	public static final String CHANGE_TYPE = "changeType";

}
