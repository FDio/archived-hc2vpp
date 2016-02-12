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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.Netmask;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.interconnection.XconnectBased;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppIetfInterfaceListener implements DataTreeChangeListener<Interface>, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(VppIetfInterfaceListener.class);

    private final ListenerRegistration<VppIetfInterfaceListener> registration;
    private final DataBroker db;
    private final vppApi api;

    private enum DataChangeType {
        CREATE, UPDATE, DELETE
    }

    /**
     * TODO-ADD-JAVADOC.
     */
    public VppIetfInterfaceListener(final DataBroker db, final vppApi api) {
        this.db = db;
        this.api = api;
        InstanceIdentifier<Interface> iid = InstanceIdentifier
                .create(Interfaces.class)
                .child(Interface.class);
        LOG.info("VPPCFG-INFO: Register listener for VPP Ietf Interface data changes");

        DataTreeIdentifier<Interface> path =
                new DataTreeIdentifier<Interface>(LogicalDatastoreType.CONFIGURATION, iid);

        registration = this.db.registerDataTreeChangeListener(path, this);
    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<Interface>> changes) {
        LOG.info("VPPCFG-INFO: swIf onDataTreeChanged()");
        for (DataTreeModification<Interface> change: changes) {
            InstanceIdentifier<Interface> iid = change.getRootPath().getRootIdentifier();
            DataObjectModification<Interface> changeDiff = change.getRootNode();

            switch (changeDiff.getModificationType()) {
                case SUBTREE_MODIFIED:
                case WRITE:
                    // create, modify or replace
                    createOrUpdateInterface(changeDiff);
                    break;
                case DELETE:
                    deleteInterface(changeDiff);
                    break;
                default:
                    LOG.info("Unsupported change type {} for {}",
                             changeDiff.getModificationType(), iid);
            }
        }
    }

    private void vppSetVppInterfaceEthernetAndL2(final int swIfIndex,
                                                 final String swIfName,
                                                 final VppInterfaceAugmentation
                                                 vppInterface) {
        int ctxId = 0;
        int rv = -77;
        int cnt = 0;
        String apiName = "";

        LOG.info("VPPCFG-INFO: <vppSetVppInterfaceEthernetAndL2>");
        LOG.info("VPPCFG-INFO:     swIfIndex = {}", swIfIndex);
        LOG.info("VPPCFG-INFO:     swIfName  = {}", swIfName);
        LOG.info("VPPCFG-INFO:     vppInterface  = {}", vppInterface);
        LOG.info("VPPCFG-INFO: </vppSetVppInterfaceEthernetAndL2>");
        if (vppInterface != null) {
            Ethernet vppEth = vppInterface.getEthernet();
            if (vppEth != null) {
                LOG.info("VPPCFG-INFO: {} Ethernet MTU = {}",
                         swIfName, vppEth.getMtu());
                /* DAW-FIXME: Need vpe-api msg to configure the Ethernet MTU */
            }

            L2 vppL2 = vppInterface.getL2();
            if (vppL2 != null) {
                Interconnection ic = vppL2.getInterconnection();
                if (ic instanceof XconnectBased) {
                    XconnectBased xc = (XconnectBased) ic;
                    String outSwIfName = xc.getXconnectOutgoingInterface();
                    LOG.info("VPPCFG-INFO: XconnectBased");
                    LOG.info("VPPCFG-INFO:   XconnectOutgoingInterface = {}",
                             outSwIfName);

                    int outSwIfIndex = api.swIfIndexFromName(outSwIfName);
                    if (swIfIndex != -1) {
                        apiName = "api.swInterfaceSetL2Xconnect";
                        ctxId =
                            api.swInterfaceSetL2Xconnect(swIfIndex,
                                                         outSwIfIndex,
                                                         (byte)1 /* enable */);
                        LOG.info("VPPCFG-INFO: {}() : outSwIfName = {}, "
                                 + "outSwIfIndex = {}, ctxId = {}", apiName,
                                 outSwIfName, outSwIfIndex, ctxId);
                        cnt = 0;
                        rv = -77;
                        while (rv == -77) {
                            rv = api.getRetval(ctxId, 1 /* release */);
                            cnt++;
                        }
                        if (rv < 0) {
                            LOG.warn("VPPCFG-WARNING: {}() ctxId = {} failed:"
                                     + " retval = {}!", apiName, ctxId, rv);
                            /* DAW-FIXME: throw exception on failure? */
                        } else {
                            LOG.info("VPPCFG-INFO: {}() ctxId = {} retval = {}"
                                     + " after {} tries.", apiName, ctxId,
                                     rv, cnt);
                        }

                    } else {
                        LOG.warn("VPPCFG-WARNING: Unknown Outgoing Interface ({})"
                                 + " specified", outSwIfName);
                    }

                } else if (ic instanceof BridgeBased) {
                    BridgeBased bb = (BridgeBased) ic;
                    String bdName = bb.getBridgeDomain();
                    int bdId = api.bridgeDomainIdFromName(bdName);
                    if (bdId > 0) {
                        byte bvi =
                            bb.isBridgedVirtualInterface() ? (byte) 1 : (byte) 0;
                        byte shg = bb.getSplitHorizonGroup().byteValue();

                        LOG.info("VPPCFG-INFO: BridgeBased");
                        LOG.info("VPPCFG-INFO:   BridgeDomain = {}, bdId = {}",
                                 bdName, bdId);
                        LOG.info("VPPCFG-INFO:   SplitHorizonGroup = {}",
                                 shg);
                        LOG.info("VPPCFG-INFO:   isBridgedVirtualInterface = {}",
                                 bvi);

                        apiName = "api.swInterfaceSetL2Bridge";
                        ctxId =
                            api.swInterfaceSetL2Bridge(swIfIndex,
                                                       bdId, shg, bvi,
                                                       (byte)1 /* enable */);
                        LOG.info("VPPCFG-INFO: {}() : bdId = {}, shg = {}, bvi = {}, ctxId = {}", apiName, bdId,
                                 shg, bvi, ctxId);
                        cnt = 0;
                        rv = -77;
                        while (rv == -77) {
                            rv = api.getRetval(ctxId, 1 /* release */);
                            cnt++;
                        }
                        if (rv < 0) {
                            LOG.warn("VPPCFG-WARNING:{}() ctxId = {} failed: retval = {}!", apiName, ctxId, rv);
                            /* DAW-FIXME: throw exception on failure? */
                        } else {
                            LOG.info("VPPCFG-INFO: {}() ctxId = {} retval = {} after {} tries.", apiName, ctxId,
                                     rv, cnt);
                        }

                    } else {
                        LOG.error("VPPCFG-ERROR: Bridge Domain {} does not exist!", bdName);
                    }

                } else {
                    LOG.error("VPPCFG-ERROR: unknonwn interconnection type!");
                }
            }
        }
    }

    /**
     * TODO-ADD-JAVADOC.
     */
    public static int parseIp(final String address) {
        int result = 0;

        // iterate over each octet
        for (String part : address.split("\\.")) {
            // shift the previously parsed bits over by 1 byte
            result = result << 8;
            // set the low order bits to the current octet
            result |= Integer.parseInt(part);
        }
        return result;
    }

    private void createVxlanTunnel(final String swIfName, final Vxlan vxlan) {
        Ipv4Address srcAddress = vxlan.getSrc();
        Ipv4Address dstAddress = vxlan.getDst();

        int srcAddr = parseIp(srcAddress.getValue());
        int dstAddr = parseIp(dstAddress.getValue());
        int encapVrfId = vxlan.getEncapVrfId().intValue();
        int vni = vxlan.getVni().getValue().intValue();

        int ctxId = api.vxlanAddDelTunnel((byte)1 /* is add */, srcAddr, dstAddr, encapVrfId, -1, vni);
        String apiName = "api.vxlanAddDelTunnel";
        LOG.info("VPPCFG-INFO: {}({}, src: {}, dst: {} enabled ([]), ...) : ctxId = {}",
            apiName, swIfName, srcAddress.getValue(), dstAddress.getValue(), ctxId);

        /* need to wait for creation of interface */
        int rv = -77;
        int cnt = 0;
        while (rv == -77) {
            rv = api.getRetval(ctxId, 1 /* release */);
            cnt++;
        }
        if (rv < 0) {
            LOG.warn("VPPCFG-WARNING: {}() ctxId = {} failed: retval = {}!", apiName, ctxId, rv);
            /* DAW-FIXME: throw exception on failure? */
        } else {
            LOG.info("VPPCFG-INFO: {}() ctxId = {} retval = {} after {} tries.", apiName, ctxId, rv, cnt);
        }
    }

    private static byte [] ipv4AddressNoZoneToArray(final Ipv4AddressNoZone ipv4Addr) {
        byte [] retval = new byte [4];
        String addr = ipv4Addr.getValue().toString();
        String [] dots = addr.split("\\.");

        for (int d = 3; d >= 0; d--) {
            retval[d] = (byte)(Short.parseShort(dots[3 - d]) & 0xff);
        }
        return retval;
    }

    private void vppSetInterface(final Interface swIf, final DataChangeType type,
                                 final Interface originalIf) {
        VppInterfaceAugmentation vppInterface =
            swIf.getAugmentation(VppInterfaceAugmentation.class);
        int ctxId = 0;
        int cnt = 0;
        int rv = -77;
        String apiName = "";

        /* DAW-FIXME: If type == UPDATE, use originalDataObject to get
         *            state of api parameters which have not been changed.
         *            For now, all parameters must be set at the same time.
         */
        LOG.info("VPPCFG-INFO: {} <swIf>", type);
        LOG.info("VPPCFG-INFO:    Name: {}", swIf.getName());
        LOG.info("VPPCFG-INFO:    Desc: {}", swIf.getDescription());
        java.lang.Class<? extends
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType>
            ifType = swIf.getType();
        if (ifType != null) {
            LOG.info("VPPCFG-INFO:    Type: {}", swIf.getType().getSimpleName());
        }
        LOG.info("VPPCFG-INFO: {} </swIf>", type);

        String swIfName = swIf.getName();
        int swIfIndex = api.swIfIndexFromName(swIfName);

        if ((ifType != null) && ifType.isAssignableFrom(EthernetCsmacd.class)) {
            if (swIfIndex != -1) {
                LOG.info("VPPCFG-INFO: {} : swIfIndex = {}", swIfName, swIfIndex);

                /* set vpp ethernet and l2 containers */
                vppSetVppInterfaceEthernetAndL2(swIfIndex, swIfName,
                                                vppInterface);

                byte enabled = swIf.isEnabled() ? (byte) 1 : (byte) 0;
                apiName = "api.swInterfaceSetFlags";
                ctxId = api.swInterfaceSetFlags(swIfIndex,
                                                enabled,
                                                enabled,
                                                (byte)0 /* deleted */);
                LOG.info("VPPCFG-INFO: {}({} ([]), enabled ([]), ...) : ctxId = {}", apiName, swIfName, swIfIndex,
                         enabled, ctxId);
                cnt = 0;
                rv = -77;
                while (rv == -77) {
                    rv = api.getRetval(ctxId, 1 /* release */);
                    cnt++;
                }
                if (rv < 0) {
                    LOG.warn("VPPCFG-WARNING: api.swInterfaceSetFlags() ctxId = {} failed: retval = {}!", ctxId, rv);
                    /* DAW-FIXME: throw exception on failure? */
                } else {
                    LOG.info("VPPCFG-INFO: {}() ctxId = {} retval = {} after {} tries.", apiName, ctxId, rv, cnt);
                }
            } else {
                LOG.error("VPPCFG-ERROR: {} not found!", swIf.getType().getSimpleName());
                LOG.error("VPPCFG-ERROR: cannot create {} type interfaces : ignoring create request for {} !",
                         swIf.getType().getSimpleName(), swIf.getName());
            }

        } else if ((ifType != null)
                    && ifType.isAssignableFrom(VxlanTunnel.class)) {
            LOG.info("VPPCFG-INFO: VxLAN tunnel configuration");

            // TODO: check name of interface, make use of renumber to change vpp
            //       interface name to desired one

            if (swIfIndex != -1) {
                // interface exists in vpp
                if (type == DataChangeType.DELETE) {
                    // TODO
                } else {
                    // TODO
                    Vxlan vxlan = vppInterface.getVxlan();

                    LOG.info("Vxlan update: {}", vxlan);
                }
            } else {
                // interface does not exist in vpp
                if (type == DataChangeType.DELETE) {
                    // cannot delete non existent interface
                    LOG.error("VPPCFG-ERROR: Cannot delete non existing interface ({})", swIf.getName());
                } else {
                    Vxlan vxlan = vppInterface.getVxlan();

                    createVxlanTunnel(swIfName, vxlan);

                    // refresh interfaces to be able to get ifIndex
                    api.swInterfaceDump((byte)1, "vxlan".getBytes());

                    int newSwIfIndex = api.swIfIndexFromName(swIfName);

                    /* set vpp ethernet and l2 containers */
                    vppSetVppInterfaceEthernetAndL2(newSwIfIndex,
                                                    swIfName,
                                                    vppInterface);

                    byte enabled = swIf.isEnabled() ? (byte) 1 : (byte) 0;
                    ctxId = api.swInterfaceSetFlags(newSwIfIndex,
                                                    enabled,
                                                    enabled,
                                                    (byte)0 /* deleted */);

                    swIfIndex = newSwIfIndex;

                    apiName = "api.swInterfaceSetFlags";
                    LOG.info("VPPCFG-INFO: {}({} ({}), enabled ({}), ...) : ctxId = {}", apiName, swIfName,
                             newSwIfIndex, enabled, ctxId);
                    cnt = 0;
                    rv = -77;
                    while (rv == -77) {
                        rv = api.getRetval(ctxId, 1 /* release */);
                        cnt++;
                    }
                    if (rv < 0) {
                        LOG.warn("VPPCFG-WARNING: {}() ctxId = {} failed: retval = {}!", apiName, ctxId, rv);
                        /* DAW-FIXME: throw exception on failure? */
                    } else {
                        LOG.info("VPPCFG-INFO: {}() ctxId = {} retval = {} after {} tries.", apiName, ctxId, rv, cnt);
                    }
                }
            }

        /* DAW-FIXME: Add additional interface types here.
         *
         * } else if ((ifType != null) && ifType.isAssignableFrom(*.class)) {
         */
        } else if (ifType != null) {
            LOG.error("VPPCFG-ERROR: Unsupported interface type ({}) : {} cannot be created!", ifType.getSimpleName(),
                      swIf.getName());
        }

        if (swIfIndex == -1) {
            LOG.warn("VPPCFG-INFO: Unknown Interface {}", swIfName);
            return;
        }

        if (swIf.getDescription() != null) {
            api.setInterfaceDescription(swIfName, swIf.getDescription());
        } else {
            api.setInterfaceDescription(swIfName, "");
        }
        Routing rt = vppInterface.getRouting();
        int vrfId = (rt != null) ? rt.getVrfId().intValue() : 0;
        LOG.info("VPPCFG-INFO: vrfId = {}", vrfId);
        if (vrfId > 0) {
            apiName = "api.swInterfaceSetTable";
            ctxId = api.swInterfaceSetTable(swIfIndex,
                                            (byte)0, /* isIpv6 */
                                            vrfId);
            LOG.info("VPPCFG-INFO: {}({} ([]), 0 /* isIpv6 */, {} /* vrfId */)"
                     + " : ctxId = {}", apiName, swIfName, swIfIndex,
                     vrfId, ctxId);
            cnt = 0;
            rv = -77;
            while (rv == -77) {
                rv = api.getRetval(ctxId, 1 /* release */);
                cnt++;
            }
            if (rv < 0) {
                LOG.warn("VPPCFG-WARNING: api.swInterfaceSetTable() ctxId = {} failed: retval = {}!", ctxId, rv);
                /* DAW-FIXME: throw exception on failure? */
            } else {
                LOG.info("VPPCFG-INFO: {}() ctxId = {} retval = {} after {} tries.", apiName, ctxId, rv, cnt);
            }
        }

        Interface1 ipIf = swIf.getAugmentation(Interface1.class);
        LOG.info("VPPCFG-INFO: ipIf = {}", ipIf);
        if (ipIf != null) {
            Ipv4 v4 = ipIf.getIpv4();
            if (v4 != null) {
                LOG.info("VPPCFG-INFO: v4 = {}", v4);

                for (Address v4Addr : v4.getAddress()) {
                    Subnet subnet = v4Addr.getSubnet();

                    if (subnet instanceof PrefixLength) {
                        Short plen = ((PrefixLength)subnet).getPrefixLength();
                        byte [] addr = ipv4AddressNoZoneToArray(v4Addr.getIp());

                        if ((plen > 0) && (addr != null)) {
                            apiName = "api.swInterfaceAddDelAddress";
                            ctxId =
                                api.swInterfaceAddDelAddress(swIfIndex,
                                                             (byte)1 /* isAdd */,
                                                             (byte)0 /* isIpv6 */,
                                                             (byte)0 /* delAll */,
                                                             plen.byteValue(), addr);
                            LOG.info("VPPCFG-INFO: {}({}/{}) to {} ({}): {}() returned ctxId = {}", apiName, addr,
                                     plen, swIfName, swIfIndex, ctxId);
                            cnt = 0;
                            rv = -77;
                            while (rv == -77) {
                                rv = api.getRetval(ctxId, 1 /* release */);
                                cnt++;
                            }
                            if (rv < 0) {
                                LOG.warn("VPPCFG-WARNING: {}() ctxId = {} failed: retval = {}!", apiName,
                                         ctxId, rv);
                                /* DAW-FIXME: throw exception on failure? */
                            } else {
                                LOG.info("VPPCFG-INFO: {}() ctxId = {} retval = {} after {} tries.", apiName,
                                         ctxId, rv, cnt);
                            }
                        } else {
                            LOG.warn("VPPCFG-WARNING: Malformed ipv4 address ({}/{}) "
                                     + "specified for {} ({}): ignoring config!",
                                     addr, plen, swIfName, swIfIndex);
                        }
                    } else if (subnet instanceof Netmask) {
                        LOG.warn("VPPCFG-WARNING: Unsupported ipv4 address subnet type 'Netmask' "
                                  + "specified for {} ({}): ignoring config!",
                                 swIfName, swIfIndex);
                    } else {
                        LOG.error("VPPCFG-ERROR: Unknown ipv4 address subnet type "
                                  + "specified for {} ({}): ignoring config!",
                                  swIfName, swIfIndex);
                    }
                }
            }

            Ipv6 v6 = ipIf.getIpv6();
            if (v6 != null) {
                LOG.info("VPPCFG-INFO: v6 = {}", v6);

                // DAW-FIXME: Add Ipv6 address support.
                LOG.warn("VPPCFG-WARNING: Ipv6 address support TBD: ignoring config!");
            }
        }
    }

    private void createOrUpdateInterface(final DataObjectModification<Interface> changeDiff) {
        if (changeDiff.getDataBefore() == null) {
            // create
            vppSetInterface(changeDiff.getDataAfter(),
                    DataChangeType.CREATE, null);
        } else {
            // update
            vppSetInterface(changeDiff.getDataAfter(),
                    DataChangeType.UPDATE,
                    changeDiff.getDataBefore());
        }
    }

    private static void deleteInterface(final DataObjectModification<Interface> changeDiff) {
        Interface swIf = changeDiff.getDataBefore();
        LOG.info("VPPCFG-INFO: <swIf>");
        LOG.info("VPPCFG-INFO:    Name: {},", swIf.getName());
        LOG.info("VPPCFG-INFO:    Desc: {}", swIf.getDescription());
        LOG.info("VPPCFG-INFO:    Type: {}", swIf.getType().getSimpleName());
        LOG.info("VPPCFG-INFO: </swIf>");

        if (swIf.getType().isAssignableFrom(EthernetCsmacd.class)) {
            LOG.error("VPPCFG-ERROR: {} Interface {} cannot be deleted!",
                     swIf.getType().getSimpleName(),
                     swIf.getName());

        /* DAW-FIXME: Add additional interface types here.
         *
         * } else if (swIf.getType().isAssignableFrom(*.class)) {
         */

        } else {
            LOG.error("VPPCFG-ERROR: Unsupported interface type ({}) : {} cannot be deleted!",
                swIf.getType().getSimpleName(),
                swIf.getName());
        }
    }

    @Override
    public void close() throws Exception {
        registration.close();
    }
}
