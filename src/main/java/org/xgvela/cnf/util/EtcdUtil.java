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

import com.google.common.base.Charsets;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class EtcdUtil {
    private static Logger LOG = LogManager.getLogger(EtcdUtil.class);

    private static String endPoints = "http://" + String.valueOf(System.getenv("ETCD_SVC_FQDN"));

    private static Client etcdClient;

    public void initClient() {
        LOG.debug("Etcd client is null, initializing...");
        try {
            etcdClient = Client.builder().endpoints(endPoints.split(",")).build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public Client getClient() {
        if (etcdClient == null) {
            initClient();
        }
        return etcdClient;
    }

    public void putKeyWithData(String key, byte[] data) {
        getClient().getKVClient().put(ByteSequence.from(key, Charsets.UTF_8), ByteSequence.from(data));
    }

    public void putKeyWithLease(String key, byte[] data, long ttl) throws Exception {
        Lease lease = getClient().getLeaseClient();
        KV kv = getClient().getKVClient();
        long leaseID = lease.grant(ttl).get().getID();
        kv.put(ByteSequence.from(key, Charsets.UTF_8), ByteSequence.from(data), PutOption.newBuilder().withLeaseId(leaseID).build());
    }

    public void removeKey(String key) {
        removeKey(ByteSequence.from(key.getBytes()));
    }

    public void removeKey(ByteSequence key) {
        LOG.info("Removing ETCD key: " + key.toString(Charsets.UTF_8));
        Client etcdClient = getClient();
        KV kvClient = etcdClient.getKVClient();
        DeleteResponse delResp = null;
        try {
            delResp = kvClient.delete(key).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage() + "\n" + e.getCause());
        }
        if (delResp != null && delResp.getDeleted() > 0) {
            LOG.debug("Key deleted successfully.");
        } else if (delResp != null && delResp.getDeleted() == 0)
            LOG.error("Unable to delete key: " + key.toString(Charsets.UTF_8));
        kvClient.close();
    }

    public void deletePool(String namespace, String name, String uid) {
        LOG.info("Deleting Etcd pool series with Namespace: [" + namespace + "], NfServiceId: [" + name + "], UID: ["
                + uid + "]");
        Client etcdClient = getClient();
        String prefix = namespace + "/" + name + "/" + uid;
        ByteSequence prefixBs = ByteSequence.from(prefix.getBytes());
        GetOption getOption = GetOption.newBuilder().withPrefix(prefixBs).build();

        try {
            GetResponse getResponse = etcdClient.getKVClient().get(prefixBs, getOption).get();
            if (getResponse.getKvs().size() == 0) {
                LOG.info("Keys already removed or not applicable for NF");
            } else {
                getResponse.getKvs().forEach(kv -> {
                    removeKey(kv.getKey());
                });
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage(), e);
        }

        String parentKey = namespace + "/" + uid + "/generatekey";
        LOG.info("Deleting parent key");
        removeKey(parentKey);
    }
}
