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

import org.xgvela.cnf.topo.ManagedElement;
import org.xgvela.cnf.topo.NFService;
import org.xgvela.cnf.topo.NFServiceInstance;
import org.xgvela.cnf.topo.NetworkFunction;

public class ApiUtil {

    public static ApiManagedElement ConvertMeToMeApi(ManagedElement me) {
        ApiManagedElement apiManagedElement = new ApiManagedElement();
        apiManagedElement.setId(me.getId());
        apiManagedElement.setDnPrefix(me.getDnPrefix());
        apiManagedElement.setUserLabel(me.getUserLabel());
        apiManagedElement.setLocationName(me.getLocationName());
        apiManagedElement.setManagedBy(me.getManagedBy());
        apiManagedElement.setVendorName(me.getVendorName());
        apiManagedElement.setUserDefinedState(me.getUserDefinedState());
        apiManagedElement.setSwVersion(me.getSwVersion());
        return apiManagedElement;
    }

    public static ApiNetworkFunction ConvertNFToNFApi(NetworkFunction nf) throws Exception {
        ApiNetworkFunction apiNetworkFunction = new ApiNetworkFunction();
        apiNetworkFunction.setId(nf.getId());
        apiNetworkFunction.setName(nf.getName());
        apiNetworkFunction.setUserLabel(nf.getUserLabel());
        apiNetworkFunction.setNfType(nf.getNfType());
        apiNetworkFunction.setState(nf.getState());
        apiNetworkFunction.setSwVersion(nf.getSwVersion());
        apiNetworkFunction.setAdministrativeState(nf.getAdministrativeState());
        apiNetworkFunction.setOperationalState(nf.getOperationalState());
        apiNetworkFunction.setUsageState(nf.getUsageState());
        apiNetworkFunction.setExtendedAttrs(nf.getExtendedAttrs());
        return apiNetworkFunction;
    }

    public static ApiNFService ConvertSVCToSvcApi(String nfId, NFService svc) throws Exception {
        ApiNFService apiSVC = new ApiNFService();
        apiSVC.setId(svc.getId());
        apiSVC.setName(svc.getName());
        apiSVC.setUserLabel(svc.getUserLabel());
        apiSVC.setNfServiceType(svc.getNfServiceType());
        apiSVC.setState(svc.getState());
        apiSVC.setSwVersion(svc.getSwVersion());
        apiSVC.setAdminstrativeState(svc.getAdminstrativeState());
        apiSVC.setOperationalState(svc.getOperationalState());
        apiSVC.setUsageState(svc.getUsageState());
        apiSVC.setHaEnabled(svc.isHaEnabled());
        apiSVC.setNumStandby(svc.getNumStandby());
        apiSVC.setMonitoringMode(svc.getMonitoringMode());
        apiSVC.setMode(svc.getMode());
        apiSVC.setExtendedAttrs(svc.getExtendedAttrs());
        return apiSVC;
    }




    public static ApiNFServiceInstance ConvertNFToSvcInstanceApi(NFServiceInstance svc) {
        ApiNFServiceInstance apiSVCInst = new ApiNFServiceInstance();
        apiSVCInst.setId(svc.getId());
        apiSVCInst.setName(svc.getName());
        apiSVCInst.setDnPrefix(svc.getDnPrefix());
        apiSVCInst.setUserLabel(svc.getUserLabel());
        apiSVCInst.setState(svc.getState());
        apiSVCInst.setHaRole(svc.getHaRole());
        apiSVCInst.setMsUid(svc.getMsUid());
        apiSVCInst.setNws(svc.getNws());
        apiSVCInst.setExtendedAttrs(svc.getExtendedAttrs());
        return apiSVCInst;
    }



}
