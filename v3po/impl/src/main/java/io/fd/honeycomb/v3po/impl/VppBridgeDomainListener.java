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

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppBridgeDomainListener implements DataTreeChangeListener<BridgeDomain>,
                                                AutoCloseable {
    private static final Logger LOG = 
        LoggerFactory.getLogger(VppBridgeDomainListener.class);
    private ListenerRegistration<VppBridgeDomainListener> registration;
    private DataBroker db;
    private vppApi api;

    private enum DataChangeType {
        CREATE, UPDATE, DELETE
    }

    /**
     * TODO-ADD-JAVADOC.
     */
    public VppBridgeDomainListener(DataBroker db, vppApi api) {
        this.db = db;
        this.api = api;
        InstanceIdentifier<BridgeDomain> iid = InstanceIdentifier
                .create(Vpp.class)
                .child(BridgeDomains.class)
                .child(BridgeDomain.class);
        LOG.info("VPPCFG-INFO: Register listener for VPP Bridge Domain data changes");
        
        DataTreeIdentifier<BridgeDomain> path = 
                new DataTreeIdentifier<BridgeDomain>(LogicalDatastoreType.CONFIGURATION, iid);
        registration = this.db.registerDataTreeChangeListener(path, this);
    }

    @Override 
    public void onDataTreeChanged(Collection<DataTreeModification<BridgeDomain>> changes) { 
        
        for (DataTreeModification<BridgeDomain> change: changes) {
            InstanceIdentifier<BridgeDomain> iid = change.getRootPath().getRootIdentifier();
            DataObjectModification<BridgeDomain> changeDiff = change.getRootNode();
            
            switch (changeDiff.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    // create, modify or replace
                    createOrUpdateBridgeDomain(changeDiff);
                    break;
                case DELETE:
                    deleteBridgeDomain(changeDiff);
                    break;
                default:
                    LOG.info("Unsupported change type {} for {}",
                             changeDiff.getModificationType(), iid);
            }
        }
    } 
    
    // handles only CREATE and UPDATE calls
    private void vppSetBridgeDomain(BridgeDomain bridgeDomain, DataChangeType type, 
                                    BridgeDomain originalBridgeDomain) {
        int rv = -77;
        int cnt = 0;
        String bdName = bridgeDomain.getName();
        int bdId = api.findOrAddBridgeDomainId(bdName);

        LOG.info("VPPCFG-INFO: {} <bridgeDomain>", type);
        LOG.info("VPPCFG-INFO:    Name: " + bdName);
        LOG.info("VPPCFG-INFO:    Flood: {} ", bridgeDomain.isFlood());
        LOG.info("VPPCFG-INFO:    Forward: {} ", bridgeDomain.isForward());
        LOG.info("VPPCFG-INFO:    Learn: {} ", bridgeDomain.isLearn());
        LOG.info("VPPCFG-INFO:    UnknownUnicastFlood: {} ",
                 bridgeDomain.isUnknownUnicastFlood());
        LOG.info("VPPCFG-INFO:    ArpTermination: {} ",
                 bridgeDomain.isArpTermination());
        LOG.info("VPPCFG-INFO: {} </bridgeDomain>", type);

        switch (type) {
            case CREATE:
            case UPDATE:
                byte flood = bridgeDomain.isFlood() ? (byte) 1 : (byte) 0;
                byte forward = bridgeDomain.isForward() ? (byte) 1 : (byte) 0;
                byte learn = bridgeDomain.isLearn() ? (byte) 1 : (byte) 0;
                byte uuf = bridgeDomain.isUnknownUnicastFlood() ? (byte) 1 : (byte) 0;
                byte arpTerm = bridgeDomain.isArpTermination() ? (byte) 1 : (byte) 0;
                if ((bdId == -1) || (bdId == 0)) {
                    LOG.warn("VPPCFG-WARNING: Bridge Domain create/lookup failed"
                             + " (bdId = {})!  Ignoring vppSetBridgeDomain request {}",
                             bdId, type);
                    return;
                } else {
                    int ctxId = api.bridgeDomainAddDel(bdId, flood, forward,
                                                       learn, uuf, arpTerm,
                                                       (byte) 1 /* isAdd */);
                    LOG.info("VPPCFG-INFO: {} api.bridgeDomainAddDel({} ({})) "
                                + "ctxId = {}", type, bdName, bdId, ctxId);
                    while (rv == -77) {
                        rv = api.getRetval(ctxId, 1 /* release */);
                        cnt++;
                    }
                    LOG.info("VPPCFG-INFO: {} api.bridgeDomainAddDel({} ({})) "
                             + "retval {} after {} tries.",
                             type, bdName, bdId, rv, cnt);
                    
                    if (rv < 0) {
                        LOG.warn("VPPCFG-WARNING: {} api.bridgeDomainAddDel({}"
                                 + " ({})) failed: retval {}!",
                                 type, bdName, bdId, rv);
                        /* DAW-FIXME: throw exception on failure? */
                    }
                }
                break;
            default:
                LOG.warn("VPPCFG-WARNING: Unknown DataChangeType {}!  "
                         + "Ignoring vppSetBridgeDomain request", type);
                return;
        }
        bdId = api.bridgeDomainIdFromName(bdName);
        LOG.info("VPPCFG-INFO: {} api.bridgeDomainIdFromName({}) = {}",
                 type, bdName, bdId);
    }

    private void createOrUpdateBridgeDomain(DataObjectModification<BridgeDomain> changeDiff) {
        if (changeDiff.getDataBefore() == null) {
            vppSetBridgeDomain(changeDiff.getDataAfter(),
                    DataChangeType.CREATE, null);
        } else {
            vppSetBridgeDomain(changeDiff.getDataAfter(),
                    DataChangeType.UPDATE, 
                    changeDiff.getDataBefore());
        }
    }

    // handles DELETE calls
    private void deleteBridgeDomain(DataObjectModification<BridgeDomain> changeDiff) {
        DataChangeType type = DataChangeType.DELETE;
        BridgeDomain bridgeDomain = changeDiff.getDataBefore();
        String bdName = bridgeDomain.getName();
        int rv = -77;
        int cnt = 0;

        LOG.info("VPPCFG-INFO: {} <bridgeDomain>", type);
        LOG.info("VPPCFG-INFO:    Name: " + bdName);
        LOG.info("VPPCFG-INFO:    Flood: {} ", bridgeDomain.isFlood());
        LOG.info("VPPCFG-INFO:    Forward: {} ", bridgeDomain.isForward());
        LOG.info("VPPCFG-INFO:    Learn: {} ", bridgeDomain.isLearn());
        LOG.info("VPPCFG-INFO:    UnknownUnicastFlood: {} ",
                 bridgeDomain.isUnknownUnicastFlood());
        LOG.info("VPPCFG-INFO:    ArpTermination: {} ",
                 bridgeDomain.isArpTermination());
        LOG.info("VPPCFG-INFO: {} </bridgeDomain>", type);

        int bdId = api.findOrAddBridgeDomainId(bdName);
        if ((bdId == -1) || (bdId == 0)) {
            LOG.warn("VPPCFG-WARNING: Unknown Bridge Domain {} "
                     + " (bdId = {})!  Ignoring vppSetBridgeDomain request {}",
                     bdName, bdId, type);
            return;
        } else {
            int ctxId = api.bridgeDomainAddDel(bdId, (byte) 0 /* flood */,
                                               (byte) 0 /* forward */,
                                               (byte) 0 /* learn */,
                                               (byte) 0 /* uuf */,
                                               (byte) 0 /* arpTerm */,
                                               (byte) 0 /* isAdd */);
            LOG.info("VPPCFG-INFO: {} api.bridgeDomainAddDel({} ({})) "
                        + "ctxId = {}", type, bdName, bdId, ctxId);
            while (rv == -77) {
                rv = api.getRetval(ctxId, 1 /* release */);
                cnt++;
            }
            LOG.info("VPPCFG-INFO: {} api.bridgeDomainAddDel({} ({})) "
                     + "retval {} after {} tries.",
                     type, bdName, bdId, rv, cnt);

            if (rv < 0) {
                LOG.warn("VPPCFG-WARNING: {} api.bridgeDomainAddDel({} ({}))"
                         + " failed: retval {}!", type, bdName, bdId, rv);
                /* DAW-FIXME: throw exception on failure? */
            }
        }    
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }
}
