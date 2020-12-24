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

package org.xgvela.cnf.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.xgvela.cnf.api.topo.*;
import org.xgvela.cnf.topo.ManagedElement;
import org.xgvela.cnf.topo.NFService;
import org.xgvela.cnf.topo.NFServiceInstance;
import org.xgvela.cnf.topo.NetworkFunction;
import org.xgvela.cnf.util.TopoManager;
import org.xgvela.cnf.zk.ZKManager;
import org.xgvela.cnf.zk.ZKUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ApiManager {
    private static Logger LOG = LogManager.getLogger(ApiManager.class);
    public static ApiManagedElement apiMe = null;

    public synchronized static String getTree() throws JsonProcessingException {
        //Form the tree everytime as state may change
        apiMe = getApiMe();
        return TopoManager.mapper.writeValueAsString(apiMe);
    }

    public static ApiManagedElement getApiMe() {
        try {
            byte[] meData = ZKManager.getData(ZKUtil.generatePath());
            ManagedElement me = TopoManager.mapper.readValue(meData, ManagedElement.class);
            apiMe = ApiUtil.ConvertMeToMeApi(me);
            List<String> nfs = ZKManager.getChildren(ZKUtil.generatePath());
            for (String nf : nfs) {
                byte[] nfData = ZKManager.getData(ZKUtil.generatePath(nf));
                NetworkFunction nwFunc = TopoManager.mapper.readValue(nfData, NetworkFunction.class);
                ApiNetworkFunction apiNetworkFunction = ApiUtil.ConvertNFToNFApi(nwFunc);
                //Number of services
                List<String> services = ZKManager.getChildren(ZKUtil.generatePath(nf));
                for (String service : services) {
                    byte[] serviceData = ZKManager.getData(ZKUtil.generatePath(nf, service));
                    NFService nfService = TopoManager.mapper.readValue(serviceData, NFService.class);
                    ApiNFService apiNfService = ApiUtil.ConvertSVCToSvcApi(nf,nfService);
                    List<String> nfSrvcChildren = ZKManager.getChildren(ZKUtil.generatePath(nf, service));
                    for (String nfSrvcInst : nfSrvcChildren) {
                        byte[] srvcInstanceData = ZKManager.getData(ZKUtil.generatePath(nf, service, nfSrvcInst));
                        NFServiceInstance nfServiceInstance = TopoManager.mapper.readValue(srvcInstanceData, NFServiceInstance.class);
                        ApiNFServiceInstance serviceInt = ApiUtil.ConvertNFToSvcInstanceApi(nfServiceInstance);
                        apiNfService.getElemSet().add(serviceInt);
                        apiNfService.getElem().put(serviceInt.getId(), serviceInt);

                    }
                    apiNetworkFunction.getElemSet().add(apiNfService);
                    apiNetworkFunction.getElem().put(apiNfService.getId(), apiNfService);
                }
                apiMe.getElemList().add(apiNetworkFunction);
                apiMe.getElem().put(apiNetworkFunction.getId(), apiNetworkFunction);
            }
        } catch (Exception e) {
            LOG.error("error forming the api managed element :: " + e.getMessage(), e);
        }
        return apiMe;
    }

}


