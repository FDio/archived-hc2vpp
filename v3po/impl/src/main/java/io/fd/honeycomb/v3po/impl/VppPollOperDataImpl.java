/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.honeycomb.v3po.impl;

import java.util.HashMap;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.V3poService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppPollOperDataOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppPollOperDataOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.openvpp.vppjapi.vppBridgeDomainDetails;
import org.openvpp.vppjapi.vppBridgeDomainInterfaceDetails;
import org.openvpp.vppjapi.vppL2Fib;
import org.openvpp.vppjapi.vppVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppPollOperDataImpl implements V3poService {
    private static final Logger LOG = LoggerFactory.getLogger(VppPollOperDataImpl.class);
    private vppVersion version;
    private DataBroker db;
    private int[] bdIds;
    private HashMap<Integer, vppL2Fib[]> l2fibByBdId = new HashMap<Integer, vppL2Fib[]>();

    /**
     * TODO-ADD-JAVADOC.
     */
    public VppPollOperDataImpl(DataBroker dataBroker) {
        db = dataBroker;
    }

    /**
     * TODO-ADD-JAVADOC.
     */
    public DataBroker getDataBroker() {
        return db;
    }

    /**
     * TODO-ADD-JAVADOC.
     * Update operational data and return string of space separated
     * interfaces names
     */
    public String updateOperational() {
        V3poApiRequest api = new V3poApiRequest(this);
        version = api.getVppVersion();
        
        bdIds = api.bridgeDomainDump(-1);
        
        // TODO: we don't need to cache BDs now that we got rid of callbacks
        l2fibByBdId.clear();
        if (bdIds != null) {
            for (int idx = 0; idx < bdIds.length; idx++) {
                l2fibByBdId.put(bdIds[idx], api.l2FibTableDump(bdIds[idx]));
            }
        }
        api.swInterfaceDumpAll();
        
        // build vpp-state
        VppStateCustomBuilder stateBuilder = new VppStateCustomBuilder();
        
        // bridge domains
        for (int i = 0; i < bdIds.length; i++) {
            vppBridgeDomainDetails bd = api.getBridgeDomainDetails(bdIds[i]);
            VppStateBridgeDomainBuilder bdBuilder = 
                    new VppStateBridgeDomainBuilder(
                            bd.name, bd.flood, bd.uuFlood, 
                            bd.arpTerm, bd.forward, bd.learn);
            
            for (int ifIdx = 0; ifIdx < bd.interfaces.length; ifIdx++) {
                vppBridgeDomainInterfaceDetails bdIf = bd.interfaces[ifIdx];
                bdBuilder.addInterface(bdIf.interfaceName, 
                        bd.bviInterfaceName == bdIf.interfaceName, 
                        bdIf.splitHorizonGroup);
            }
            
            vppL2Fib[] bdFibs = l2fibByBdId.get(bdIds[i]);

            for (int fibIdx = 0; fibIdx < bdFibs.length; fibIdx++) {
                vppL2Fib fib = bdFibs[fibIdx];
                bdBuilder.addL2Fib(fib.filter, fib.bridgedVirtualInterface,
                                   fib.outgoingInterface, fib.physAddress,
                                   fib.staticConfig);
            }
            
            stateBuilder.addBridgeDomain(bdBuilder.build());
        }
        
        stateBuilder.setVersion(version);
        
        // write to oper
        writeVppState(getVppStateIid(), stateBuilder.build());
        
        return api.ifNames;
    }
    
    @Override
    public Future<RpcResult<VppPollOperDataOutput>> vppPollOperData() {
        String ifNames = updateOperational();
        

        VppPollOperDataOutput output = new VppPollOperDataOutputBuilder()
            .setStatus(new Long(1)).build();

        return RpcResultBuilder.success(output).buildFuture();
    }
    
    private InstanceIdentifier<VppState> getVppStateIid() {
        return (InstanceIdentifier.create(VppState.class));
    }
    
    private void writeVppState(InstanceIdentifier<VppState> iid, VppState vppState) {
        WriteTransaction transaction = db.newWriteOnlyTransaction();

        //LOG.info("VPPOPER-INFO: Writing vpp-state to oper DataStore.");
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, vppState);
        
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<Void>(
                "VPPOPER-WARNING: Failed to write vpp-state to oper datastore", LOG));
    }
}

