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

package org.xgvela.cnf.k8s;

import com.fasterxml.jackson.databind.JsonNode;
import org.xgvela.cnf.Constants;
import org.xgvela.cnf.topo.PodNetworksStatus;
import org.xgvela.cnf.topo.PodPhase;
import org.xgvela.cnf.topo.State;
import org.xgvela.cnf.util.TopoManager;
import org.xgvela.cnf.zk.ZKManager;
import org.xgvela.cnf.zk.ZKUtil;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.DaemonSet;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.KubernetesClientTimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
public class K8sUtil {

	private static final Logger LOG = LogManager.getLogger(K8sUtil.class);

	private static KubernetesClient client;

	private static String podId = String.valueOf(System.getenv("K8S_POD_ID"));
	private static String podNamespace = String.valueOf(System.getenv("K8S_NAMESPACE"));
	private static String URL = "https://" + String.valueOf(System.getenv("K8S_SVC_FQDN"));

	public class RobinRole {
	}

	private void newClient() {
		LOG.info("Initializing Kubernetes client with URL: " + URL);
		client = new DefaultKubernetesClient(URL);
	}

	public KubernetesClient getClient() {
		if (client == null) {
			newClient();
		}
		return client;
	}

	public String getNfSwVersion(String name) {
		Namespace namespace = getNamespace(name);
		if (namespace != null && namespace.getMetadata() != null && namespace.getMetadata().getAnnotations() != null)
			return namespace.getMetadata().getAnnotations().getOrDefault("svcVersion", "v0");
		return "v0";
	}

	private Namespace getNamespace(String name) {
		LOG.debug("Getting Namespace: " + name);
		Namespace namespace = null;
		try {
			namespace = getClient().namespaces().withName(name).get();
		} catch (KubernetesClientException e) {
			LOG.error(e.getMessage(), e);
		}
		return namespace;
	}

	public static boolean IsNfByNamespaceInstanceCountCalculated(String nfName, String namespace) {
		return ZKManager.PathExist(ZKUtil.generatePathForNFCountCalculation(nfName, namespace));
	}

	public static void removeAllNetworkFunctionCount() {
		ZKManager.delete(ZKUtil.generatePathForNFCountCalculation());
	}

	public static void addNetworkFunctionCount(String nfname, String namespace) throws Exception {
		// cache namespaces for whom nf service count has been calculated
		ZKManager.updateData(ZKUtil.generatePathForNFCountCalculation(nfname, namespace), "".getBytes());
	}

	public static void removeNetworkFunctionCount(String nfname, String namespace) throws Exception {
		// cache namespaces for whom nf service count has been calculated
		ZKManager.delete(ZKUtil.generatePathForNFCountCalculation(nfname, namespace));
	}

	/*
	 * TODO: remove cache workaround for XGVela network function ( 2 namespaces )
	 * TODO: handle policy based Critical NF Services decisions
	 */
	public int getNfInstanceCount(String namespace, String nfId, int currentCount, boolean... recalculate)
			throws Exception {

		// already calculated once
		if (K8sUtil.IsNfByNamespaceInstanceCountCalculated(nfId, namespace) && recalculate.length == 0) {

			LOG.info("NF Service Count for Namespace: " + namespace + ", nfId: " + nfId + " is: " + currentCount);
			return currentCount;

		} else if (recalculate.length != 0) {
			LOG.info("Recalculating count, triggered by possible NF state change");
		}

		// filter service based on matching nfId and xgvelaId
		Predicate<ObjectMeta> isValidNf = meta -> {
			try {
				JsonNode tmaas = TopoManager.mapper.readTree(meta.getAnnotations().get(Constants.ANNOTATION_TMAAS));
				if (tmaas.get(Constants.NF_ID).asText().equals(nfId)
						&& tmaas.get(Constants.XGVELA_ID).asText().equals(TopoManager.xgvelaId))
					return true;
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			return false;
		};

		// filter for tmaas annotations
		Predicate<ObjectMeta> hasTMaaS = meta -> meta.getAnnotations() != null
				&& meta.getAnnotations().containsKey(Constants.ANNOTATION_TMAAS);

		LOG.debug("Getting number of Deployments, StatefulSets and DaemonSets for Namespace: " + namespace);
		KubernetesClient client = getClient();
		int services = 0;
		try {
			services += client.apps().deployments().inNamespace(namespace).list().getItems().parallelStream()
					.filter(deployment -> deployment.getSpec().getReplicas() > 0)
					.map(deployment -> deployment.getMetadata()).filter(hasTMaaS).filter(isValidNf).count();

			services += client.apps().statefulSets().inNamespace(namespace).list().getItems().parallelStream()
					.filter(statefulSet -> statefulSet.getSpec().getReplicas() > 0)
					.map(statefulSet -> statefulSet.getMetadata()).filter(hasTMaaS).filter(isValidNf).count();

			services += client.apps().daemonSets().inNamespace(namespace).list().getItems().parallelStream()
					.filter(daemonSet -> daemonSet.getStatus().getCurrentNumberScheduled() > 0)
					.map(daemonSet -> daemonSet.getMetadata()).filter(hasTMaaS).filter(isValidNf).count();

		} catch (KubernetesClientException e) {
			LOG.error(e.getMessage(), e);
		}

		LOG.info("NF Service Count for Namespace: [" + namespace + "], nfId: [" + nfId + "] is: [" + services
				+ "], Instance Count for NF is: [" + (currentCount + services) + "]");

		K8sUtil.addNetworkFunctionCount(nfId, namespace);
		return (currentCount + services);
	}

	public Pod getPod(String name, String namespace) {
		LOG.debug("Getting Pod: " + name + ", Namespace: " + namespace);
		Pod pod = null;
		try {
			pod = getClient().pods().inNamespace(namespace).withName(name).get();
		} catch (KubernetesClientException e) {
			LOG.error(e.getMessage(), e);
		}
		return pod;
	}

	public State getNfServiceInstanceState(Pod pod) {
		State state = State.NOT_READY;
		if (pod != null) {
			LOG.debug("Getting NF Service Instance State: Pod: " + pod.getMetadata().getName() + ", Namespace: "
					+ pod.getMetadata().getNamespace());

			PodPhase podPhase = PodPhase.fromValue(pod.getStatus().getPhase());
			if (pod.getStatus().getContainerStatuses().size() > 0) {
				boolean ready = true;
				for (ContainerStatus containerStatus : pod.getStatus().getContainerStatuses()) {
					LOG.debug("ContainerName: " + containerStatus.getName() + ", ContainerReady: "
							+ containerStatus.getReady());
					if (!containerStatus.getReady()) {
						ready = false;
						break;
					}
				}
				if (!ready) {
					state = State.NOT_READY;
				} else if (ready) {
					state = State.READY;
				}
				LOG.info("Atleast one container status available: PodReadyProbe: " + ready + ", PodPhase: " + podPhase
						+ ", State: " + state);
			} else {
				LOG.info("No container statuses available: PodPhase: " + podPhase + ", State: " + state);
			}
		} else {
			LOG.info("Pod does not exist anymore, NF Service Instance object will be removed from tree");
			return State.TERMINATED;
		}
		return state;
	}

	public List<PodNetworksStatus> getNetworkStatus(Pod pod) {
		LOG.info("Retrieving Network-Status for: [" + pod.getMetadata().getName() + "]");
		List<PodNetworksStatus> nws = new ArrayList<>();
		Map<String, String> annotations = pod.getMetadata().getAnnotations();
		String podIP = pod.getStatus().getPodIP();

		// k8s calico annotation
		if (annotations.containsKey(Constants.ANNOTATION_K8S_NWS_STATUS)) {
			LOG.debug(Constants.ANNOTATION_K8S_NWS_STATUS + " annotation present");
			try {
				JsonNode nwsList = TopoManager.mapper.readTree(annotations.get(Constants.ANNOTATION_K8S_NWS_STATUS));
				if (nwsList != null) {

					Iterator<JsonNode> iterator = nwsList.iterator();
					while (iterator.hasNext()) {
						JsonNode nw = iterator.next();

						PodNetworksStatus podNws = new PodNetworksStatus();
						podNws.setName(nw.get("name").asText());
						//if name is blank default it to k8s-pod-network
						if (podNws.getName().isBlank()) {
							podNws.setName("k8s-pod-network");
						}
						podNws.setIntf("eth0");
						if (nw.has("interface"))
							podNws.setIntf(nw.get("interface").asText());

						podNws.set_default(false);
						if (nw.has("default"))
							podNws.set_default(nw.get("default").asBoolean());

						List<String> ips = StreamSupport.stream(nw.get("ips").spliterator(), true)
								.map(ip -> ip.asText()).collect(Collectors.toList());
						podNws.setIps(ips);

						// in case 'default: true' is not present
						if (ips.contains(podIP))
							podNws.set_default(true);
						nws.add(podNws);
					}
				} else
					LOG.debug("No Json content to bind in K8s network annotation");
			} catch (IOException | NullPointerException e) {
				LOG.error("Json parsing failed", e);
			}
		} else {

			if (podIP != null) {
				LOG.debug(Constants.ANNOTATION_K8S_NWS_STATUS
						+ " annotation not present, setting default interface as 'eth0' and name as 'k8s-pod-network'");

				List<String> ips = new ArrayList<>();
				ips.add(podIP);

				PodNetworksStatus podNws = new PodNetworksStatus();
				podNws.setName("k8s-pod-network");
				podNws.setIntf("eth0");
				podNws.set_default(true);
				podNws.setIps(ips);

				nws.add(podNws);
			} else {
				LOG.debug("No IPs available");
			}
		}

		// cim vip annotation
		if (annotations.containsKey(Constants.ANNOTATION_VIP_STATUS)) {
			LOG.debug(Constants.ANNOTATION_VIP_STATUS + " annotation present");
			try {
				JsonNode nwsList = TopoManager.mapper.readTree(annotations.get(Constants.ANNOTATION_VIP_STATUS));
				if (nwsList != null) {

					Iterator<JsonNode> iterator = nwsList.iterator();
					while (iterator.hasNext()) {
						JsonNode nw = iterator.next();

						PodNetworksStatus podNws;
						String intf = nw.get("interface").asText();
						podNws = nws.parallelStream().filter(i -> i.getIntf().equals(intf)).findFirst().orElse(null);
						if (podNws == null) {
							podNws = new PodNetworksStatus();
							podNws.setIntf(intf);
							podNws.set_default(false);
						}
						List<String> vips = StreamSupport.stream(nw.get("vip_list").spliterator(), true)
								.map(vip -> vip.asText()).collect(Collectors.toList());

						podNws.setVips(vips);
						nws = nws.parallelStream().filter(i -> !i.getIntf().equals(intf)).collect(Collectors.toList());
						nws.add(podNws);
					}
				} else
					LOG.debug("No Json content to bind in VIP network annotation");
			} catch (IOException | NullPointerException e) {
				LOG.error("Json parsing failed", e);
			}
		}
		return nws;
	}

	public String getOwnerKind(Pod pod) {
		String ownerKind = "RobinRole";
		try {
			return pod.getMetadata().getOwnerReferences().get(0).getKind();
		} catch (Exception e) {
			LOG.debug("Unable to find Pod, return default Kind for Owner: " + ownerKind);
		}
		return ownerKind;
	}

	public Object getOwner(String microservice, String namespace, String kind) {
		LOG.debug("Getting Microservice: [" + microservice + "], Namespace: [" + namespace + "], Kind: " + kind);
		KubernetesClient client = getClient();
		switch (kind) {
		case "ReplicaSet":
			try {
				Deployment deployment = client.apps().deployments().inNamespace(namespace).withName(microservice).get();
				if (deployment != null)
					return deployment;
				else
					LOG.debug("Unable to find Deployment");
			} catch (KubernetesClientException e) {
				LOG.error(e.getMessage(), e);
			}
			break;

		case "DaemonSet":
			try {
				DaemonSet daemonSet = client.apps().daemonSets().inNamespace(namespace).withName(microservice).get();
				if (daemonSet != null)
					return daemonSet;
				else
					LOG.debug("Unable to find DaemonSet");
			} catch (KubernetesClientException e) {
				LOG.error(e.getMessage(), e);
			}
			break;

		case "StatefulSet":
			try {
				StatefulSet statefulSet = client.apps().statefulSets().inNamespace(namespace).withName(microservice)
						.get();
				if (statefulSet != null)
					return statefulSet;
				else
					LOG.debug("Unable to find StatefulSet");
			} catch (KubernetesClientException e) {
				LOG.error(e.getMessage(), e);
			}
			break;

		case "RobinRole":
			LOG.debug("NF Service Kind: RobinRole");
			return new RobinRole();
		}
		return null;
	}

	public int getNfServiceInstanceCount(Deployment deployment) {
		LOG.debug("Instance Count for Deployment: " + deployment.getSpec().getReplicas());
		return deployment.getSpec().getReplicas();
	}

	public int getNfServiceInstanceCount(DaemonSet daemonSet) {
		LOG.debug("Instance Count for DaemonSet: " + daemonSet.getStatus().getCurrentNumberScheduled());
		return daemonSet.getStatus().getCurrentNumberScheduled();
	}

	public int getNfServiceInstanceCount(StatefulSet statefulSet) {
		LOG.debug("Instance Count for StatefulSet: " + statefulSet.getSpec().getReplicas());
		return statefulSet.getSpec().getReplicas();
	}

	public Map<String, String> getTopoAnnotation() {
		LOG.debug("Getting pod annotations for pod: " + podId);
		return getClient().pods().inNamespace(podNamespace).withName(podId).get().getMetadata().getAnnotations();
	}

	public boolean isHAEnabled(Map<String, String> annotations) {
		if (annotations != null && annotations.containsKey(Constants.ANNOTATION_HA)) {
			LOG.debug("HA is enabled: " + annotations.get(Constants.ANNOTATION_HA) + " annotation found on deployment");
			try {
				JsonNode haDetails = TopoManager.mapper.readTree(annotations.get(Constants.ANNOTATION_HA));
				if (haDetails.has("numStandby") && haDetails.has("monitoringMode") && haDetails.has("mode")) {
					return true;
				}
				LOG.debug("HA annotations are incorrect: numStandby, monitoringMode & mode are the expected keys in "
						+ Constants.ANNOTATION_HA + " annotation");
				return false;
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
		}
		LOG.debug("HA is disabled: " + Constants.ANNOTATION_HA + " annotation not found on deployment");
		return false;
	}

	public int getNumStandby(Map<String, String> annotations) {
		try {
			JsonNode haDetails = TopoManager.mapper.readTree(annotations.get(Constants.ANNOTATION_HA));
			return haDetails.get("numStandby").asInt();
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return 0;
	}

	public String getMonitoringMode(Map<String, String> annotations) {
		try {
			JsonNode haDetails = TopoManager.mapper.readTree(annotations.get(Constants.ANNOTATION_HA));
			return haDetails.get("monitoringMode").asText();
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return Constants.EMPTY_STRING;
	}

	public String getMode(Map<String, String> annotations) {
		try {
			JsonNode haDetails = TopoManager.mapper.readTree(annotations.get(Constants.ANNOTATION_HA));
			return haDetails.get("mode").asText();
		} catch (IOException e) {
			LOG.error(e.getMessage());
		}
		return Constants.EMPTY_STRING;
	}

	public void deleteCimConfigMaps(String namespace) {
		LOG.info("Deleting CIM ConfigMaps in namespace: [" + namespace + "]");
		KubernetesClient client = getClient();

		client.configMaps().inNamespace(namespace).list().getItems().stream()
				.filter(configMap -> configMap.getMetadata().getName().endsWith("-mgmt-cfg")).forEach(configMap -> {

					LOG.info("Deleting ConfigMap: [" + configMap.getMetadata().getName() + "]");
					try {
						client.configMaps().inNamespace(namespace).withName(configMap.getMetadata().getName()).delete();
					} catch (KubernetesClientTimeoutException e) {
						LOG.error(e.getMessage());
						e.getResourcesNotReady().forEach(
								hasMetadata -> LOG.error("Name: [" + hasMetadata.getMetadata().getName() + "]"));
					} catch (KubernetesClientException e) {
						LOG.error(e.getMessage());
					}
				});
	}

	/**
	 * returns node labels
	 *
	 * @param name
	 */
	public Map<String, String> getNodeLabels(String name) {
		Node node = getNode(name);
		if (node != null) {
			return node.getMetadata().getLabels();
		}
		LOG.debug("Unable to get node labels");
		return null;
	}

	/**
	 * queries and caches node by name
	 *
	 * @param name
	 * @return
	 */
	private Node getNode(String name) {
		LOG.debug("Getting node: " + name);
		try {
			if (nodeMap.containsKey(name)) {
				LOG.debug("Getting node from internal cache");
				return nodeMap.get(name);
			}

			LOG.debug("Querying K8s APIServer");
			Node node = getClient().nodes().withName(name).get();
			nodeMap.put(name, node);
			return node;

		} catch (KubernetesClientException e) {
			LOG.error(e.getMessage() + ", Status: " + e.getStatus() + ", Code: " + e.getCode());
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		return null;
	}

	// internal cache to store nodes
	private Map<String, Node> nodeMap = new HashMap<>();

	/**
	 * merges pod and node labels to return extended attributes for nf service
	 * instance
	 *
	 * @param podLabels
	 * @param nodeLabels
	 * @return
	 */
	public Map<String, String> mergeLabels(Map<String, String> podLabels, Map<String, String> nodeLabels) {
		Map<String, String> extendedAttrs = new HashMap<String, String>();
		extendedAttrs.putAll(podLabels);

		// may be null; if not, merge with pod labels, override if any duplicate keys
		if (nodeLabels != null) {
			extendedAttrs.putAll(nodeLabels);
		}
		return extendedAttrs;
	}
}