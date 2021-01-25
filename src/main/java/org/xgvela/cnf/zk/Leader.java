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

package org.xgvela.cnf.zk;

import org.xgvela.cnf.k8s.K8sUtil;
import org.xgvela.cnf.util.TopoManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;


public class Leader extends LeaderSelectorListenerAdapter implements Closeable {

    private static final Logger LOG = LogManager.getLogger(Leader.class);

    public static final CountDownLatch leaderLatch = new CountDownLatch(1);
    private final LeaderSelector leaderSelector;

    public Leader(CuratorFramework client, String path) {
        // create a leader selector using the given path for management
        // all participants in a given leader selection must use the same path
        leaderSelector = new LeaderSelector(client, path, this);

    }

    public void start() {
        // the selection for this instance doesn't start until the leader selector is started
        // leader selection is done in the background so this call to leaderSelector.start() returns immediately
        leaderSelector.autoRequeue();
        leaderSelector.start();
    }

    @Override
    public void close() throws IOException {
        leaderSelector.close();
    }

    @Override
    public void takeLeadership(CuratorFramework client) {
        LOG.debug("this pod is selected as leader");
        try {
            if (!ZKManager.PathExist(ZKUtil.generatePath())) {
                //remove any cached entry for calculated network function counts
                K8sUtil.removeAllNetworkFunctionCount();
                ZKManager.create(ZKUtil.generatePath(), TopoManager.mapper.writeValueAsBytes(TopoManager.me));
            }
            //this function should not leave for this leader to exist
            leaderLatch.await();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            LOG.debug("the leader has relinquished");
        }

    }
}