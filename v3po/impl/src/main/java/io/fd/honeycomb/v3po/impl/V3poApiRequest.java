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

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.math.BigInteger;
import java.net.InetAddress;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.SoftwareLoopback;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.Statistics;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.StatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.IpAddressOrigin;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge64;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStatisticsAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStatisticsAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VxlanTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet.Duplex;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.EthernetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Vxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.VxlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.l2.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.l2.interconnection.XconnectBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.openvpp.vppjapi.vppBridgeDomainDetails;
import org.openvpp.vppjapi.vppBridgeDomainInterfaceDetails;
import org.openvpp.vppjapi.vppIPv4Address;
import org.openvpp.vppjapi.vppIPv6Address;
import org.openvpp.vppjapi.vppInterfaceCounters;
import org.openvpp.vppjapi.vppInterfaceDetails;
import org.openvpp.vppjapi.vppVxlanTunnelDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * VPP API Class overriding interface details callback
 */
public class V3poApiRequest {
    private static final Logger LOG = LoggerFactory.getLogger(V3poApiRequest.class);
    public String ifNames = "";
    private final VppPollOperDataImpl caller;
    private final vppApi api;

    public V3poApiRequest(final vppApi api, final VppPollOperDataImpl vppPollOperData) {
        this.api = api;
        caller = vppPollOperData;
    }

    private static InstanceIdentifier<Interface> getStateInterfaceIid(final String interfaceName) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class,
                new InterfaceKey(interfaceName));
    }

    private static InstanceIdentifier<Interface2> getStateInterfaceIpId(final InstanceIdentifier<Interface> iid) {
        return iid.augmentation(Interface2.class);
    }

    private static InstanceIdentifier<Statistics> getStateInterfaceStatsId(final InstanceIdentifier<Interface> iid) {
        return iid.child(Statistics.class);
    }

    private static Counter64 getCounter64(final long num) {
        return new Counter64(BigInteger.valueOf(num));
    }

    private static Counter32 getCounter32(final long num) {
        return new Counter32(num);
    }

    private static Statistics buildInterfaceStatistics(final vppInterfaceCounters ifCounters) {
        if (ifCounters == null) {
            return null;
        }
        StatisticsBuilder statsBuilder = new StatisticsBuilder();

        statsBuilder.setInBroadcastPkts(getCounter64(ifCounters.rxBroadcast));
        statsBuilder.setInDiscards(getCounter32(ifCounters.rxDiscard));
        statsBuilder.setInErrors(getCounter32(ifCounters.rxError));
        statsBuilder.setInMulticastPkts(getCounter64(ifCounters.rxMulticast));
        statsBuilder.setInOctets(getCounter64(ifCounters.rxOctets));
        statsBuilder.setInUnicastPkts(getCounter64(ifCounters.rxUnicast));
        statsBuilder.setInUnknownProtos(getCounter32(ifCounters.rxUnknownProto));

        statsBuilder.setOutBroadcastPkts(getCounter64(ifCounters.txBroadcast));
        statsBuilder.setOutDiscards(getCounter32(ifCounters.txDiscard));
        statsBuilder.setOutErrors(getCounter32(ifCounters.txError));
        statsBuilder.setOutMulticastPkts(getCounter64(ifCounters.txMulticast));
        statsBuilder.setOutOctets(getCounter64(ifCounters.txOctets));
        statsBuilder.setOutUnicastPkts(getCounter64(ifCounters.txUnicast));

        VppInterfaceStatisticsAugmentationBuilder statsAugBuilder =
            new VppInterfaceStatisticsAugmentationBuilder();
        statsAugBuilder.setInErrorsMiss(getCounter64(ifCounters.rxMiss));
        statsAugBuilder.setInErrorsNoBuf(getCounter64(ifCounters.rxFifoFull)); // FIXME? Is this right?
        statsAugBuilder.setOutDiscardsFifoFull(getCounter64(ifCounters.txFifoFull));

        statsBuilder.addAugmentation(VppInterfaceStatisticsAugmentation.class,
                                     statsAugBuilder.build());

        return statsBuilder.build();
    }

    private static String getMacAddress(final byte[] mac) {
        StringBuilder sb = new StringBuilder(18);
        for (byte b : mac) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static final Gauge64 vppSpeed0 = new Gauge64(BigInteger.ZERO);
    private static final Gauge64 vppSpeed1 = new Gauge64(BigInteger.valueOf(10 * 1000000));
    private static final Gauge64 vppSpeed2 = new Gauge64(BigInteger.valueOf(100 * 1000000));
    private static final Gauge64 vppSpeed4 = new Gauge64(BigInteger.valueOf(1000 * 1000000));
    private static final Gauge64 vppSpeed8 = new Gauge64(BigInteger.valueOf(10000L * 1000000));
    private static final Gauge64 vppSpeed16 = new Gauge64(BigInteger.valueOf(40000L * 1000000));
    private static final Gauge64 vppSpeed32 = new Gauge64(BigInteger.valueOf(100000L * 1000000));

    private static Gauge64 getSpeed(final byte vppSpeed) {
        switch (vppSpeed) {
            case 1: return vppSpeed1;
            case 2: return vppSpeed2;
            case 4: return vppSpeed4;
            case 8: return vppSpeed8;
            case 16: return vppSpeed16;
            case 32: return vppSpeed32;
            default: return vppSpeed0;
        }
    }

    private static String ipv4IntToString(final int ip) {
        return InetAddresses.fromInteger(ip).getHostAddress();
    }

    private Interface buildStateInterface(final int ifIndex,
                                          @Nonnull final String interfaceName,
                                          final int supIfIndex,
                                          final byte[] physAddr,
                                          final byte adminUp, final byte linkUp,
                                          final byte linkDuplex, final byte linkSpeed,
                                          final int subId, final byte subDot1ad,
                                          final byte subNumberOfTags,
                                          final int subOuterVlanId,
                                          final int subInnerVlanId,
                                          final byte subExactMatch,
                                          final byte subDefault,
                                          final byte subOuterVlanIdAny,
                                          final byte subInnerVlanIdAny,
                                          final int vtrOp, final int vtrPushDot1q,
                                          final int vtrTag1, final int vtrTag2,
                                          final Statistics stats) {
        Preconditions.checkNotNull(interfaceName, "interfaceName should not be null");
        InterfaceBuilder ifBuilder = new InterfaceBuilder();
        Class<? extends InterfaceType> ifType;

        // FIXME: missing types for virtualethernet, subinterface, tap interface etc
        if (interfaceName.startsWith("loop")) {
            ifType = SoftwareLoopback.class;
        } else if (interfaceName.startsWith("vxlan_tunnel")) {
            ifType = VxlanTunnel.class;
        } else {
            ifType = EthernetCsmacd.class;
        }
        ifBuilder.setName(interfaceName)
            .setType(ifType)
            .setAdminStatus((adminUp == 0 ? AdminStatus.Down : AdminStatus.Up))
            .setOperStatus((linkUp == 0 ? OperStatus.Down : OperStatus.Up));
/*
        DataContainerNodeBuilder<NodeIdentifierWithPredicates, MapEntryNode> builder = ImmutableNodes.mapEntryBuilder()
            .withNodeIdentifier(new NodeIdentifierWithPredicates(Interface.QNAME, NAME_QNAME, interfaceName));
        builder.withChild(ImmutableNodes.leafNode(IF_TYPE, SoftwareLoopback.QNAME))*/

        // subinterface?
        if (ifIndex != supIfIndex) {
            // TODO: get name and set
        }

        if (physAddr != null) {
            ifBuilder.setPhysAddress(new PhysAddress(getMacAddress(physAddr)));
        }
        ifBuilder.setSpeed(getSpeed(linkSpeed));

        if (stats != null) {
            ifBuilder.setStatistics(stats);
        }
        int bdId = api.bridgeDomainIdFromInterfaceName(interfaceName);
        vppBridgeDomainDetails bd = (bdId != -1 ? api.getBridgeDomainDetails(bdId) : null);

        String bdName = null;
        short splitHorizonGroup = 0;
        boolean bvi = false;

        if (bd != null) {
            bdName = bd.name;
            for (vppBridgeDomainInterfaceDetails bdIf : bd.interfaces) {
                if (!interfaceName.equals(bdIf.interfaceName)) {
                    continue;
                }
                if (interfaceName.equals(bd.bviInterfaceName)) {
                    bvi = true;
                }
                splitHorizonGroup = bdIf.splitHorizonGroup;
            }
        }

        VppInterfaceStateAugmentationBuilder vppIfStateAugBuilder =
            new VppInterfaceStateAugmentationBuilder();

        vppIfStateAugBuilder.setDescription(api.getInterfaceDescription(interfaceName));

        setStateInterfaceL2(vppIfStateAugBuilder, bdId != -1, false, null,
                            bdName, splitHorizonGroup, bvi);

        if (EthernetCsmacd.class == ifType) {
            setStateInterfaceEthernet(vppIfStateAugBuilder, linkDuplex == 2,
                                      "ACME Inc.", 1234);
        }

        vppVxlanTunnelDetails[] vxlanDet = api.vxlanTunnelDump(ifIndex);
        if (null != vxlanDet && vxlanDet.length >= 1) {
            setStateInterfaceVxlan(vppIfStateAugBuilder, vxlanDet[0].srcAddress,
                                   vxlanDet[0].dstAddress, vxlanDet[0].vni,
                                   vxlanDet[0].encapVrfId);
        }

        ifBuilder.addAugmentation(VppInterfaceStateAugmentation.class,
                                  vppIfStateAugBuilder.build());

        InterfaceStateIpv4Builder ipv4Builder = new InterfaceStateIpv4Builder();
// TODO        ipv4Builder.setMtu(1234);

        InetAddress addr = null;

        vppIPv4Address[] ipv4Addrs = api.ipv4AddressDump(interfaceName);
        if (ipv4Addrs != null) {
            for (vppIPv4Address vppAddr : ipv4Addrs) {
                if (null == vppAddr) {
                    LOG.error("ipv4 address structure in null");
                    continue;
                }

                // FIXME: vppIPv4Address and vppIPv6 address can be the same if both will use
                // byte array for ip
                byte[] bytes = Ints.toByteArray(vppAddr.ip);
                try {
                    addr = InetAddress.getByAddress(bytes);
                } catch (java.net.UnknownHostException e) {
                    e.printStackTrace();
                    continue;
                }

                ipv4Builder.addAddress(addr.getHostAddress(), vppAddr.prefixLength, IpAddressOrigin.Static);
            }
        }

        InterfaceStateIpv6Builder ipv6Builder = new InterfaceStateIpv6Builder();
// TODO        ipv6Builder.setMtu(1234);

        vppIPv6Address[] ipv6Addrs = api.ipv6AddressDump(interfaceName);
        if (ipv6Addrs != null) {
            for (vppIPv6Address vppAddr : ipv6Addrs) {
                if (null == vppAddr) {
                    LOG.error("ipv6 address structure in null");
                    continue;
                }

                byte[] bytes = vppAddr.ip;
                try {
                    addr = InetAddress.getByAddress(bytes);
                } catch (java.net.UnknownHostException e) {
                    e.printStackTrace();
                    continue;
                }

                ipv6Builder.addAddress(addr.getHostAddress(), vppAddr.prefixLength, IpAddressOrigin.Static);
            }
        }
        Interface2Builder ipBuilder = new Interface2Builder();

        ipBuilder.setIpv4(ipv4Builder.build());
        ipBuilder.setIpv6(ipv6Builder.build());

        ifBuilder.addAugmentation(Interface2.class, ipBuilder.build());

        return ifBuilder.build();
    }

    private static void setStateInterfaceL2(
            final VppInterfaceStateAugmentationBuilder augBuilder,
            final boolean isL2BridgeBased, final boolean isXconnect,
            final String xconnectOutgoingInterface,
            final String bdName, final short splitHorizonGroup, final boolean bvi) {

        L2Builder l2Builder = new L2Builder();

        if (isXconnect) {
            l2Builder.setInterconnection(
                    new XconnectBasedBuilder()
                        .setXconnectOutgoingInterface(xconnectOutgoingInterface)
                        .build());
        } else if (isL2BridgeBased) {
            l2Builder.setInterconnection(
                    new BridgeBasedBuilder()
                        .setBridgeDomain(bdName)
                        .setSplitHorizonGroup(splitHorizonGroup)
                        .setBridgedVirtualInterface(bvi)
                        .build());
        }

        augBuilder.setL2(l2Builder.build());
    }

    private static void setStateInterfaceEthernet(
            final VppInterfaceStateAugmentationBuilder augBuilder,
            final boolean isFullDuplex, final String manufacturerDesc, final Integer mtu) {

        EthernetBuilder ethBuilder = new EthernetBuilder();
        ethBuilder.setDuplex((isFullDuplex ? Duplex.Full : Duplex.Half))
            .setManufacturerDescription(manufacturerDesc)
            .setMtu(mtu);

        augBuilder.setEthernet(ethBuilder.build());
    }

    private static void setStateInterfaceVxlan(
            final VppInterfaceStateAugmentationBuilder augBuilder, final int srcAddress,
            final int dstAddress, final int vni, final int encapVrfId) {

        String srcAddressStr = ipv4IntToString(srcAddress);
        String dstAddressStr = ipv4IntToString(dstAddress);

        VxlanBuilder vxlanBuilder = new VxlanBuilder();
        Vxlan vxlan = vxlanBuilder
                .setSrc(new Ipv4AddressNoZone(srcAddressStr))
                .setDst(new Ipv4AddressNoZone(dstAddressStr))
                .setVni((long)vni)
                .setEncapVrfId((long)encapVrfId)
                .build();

        augBuilder.setVxlan(vxlan);
    }

    private void writeToIfState(final InstanceIdentifier<Interface> iid,
                                final Interface intf) {
        DataBroker db = caller.getDataBroker();
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        // TODO: how to delete existing interfaces that disappeared? (reset it before each dumpInterfaces call?)

        /*LOG.info("VPPOPER-INFO: Adding interface " + intf.getName()
                 + " to oper DataStore.");*/
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, intf);

        CheckedFuture<Void, TransactionCommitFailedException> future =
            transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<Void>(
                "VPPOPER-WARNING: Failed to write "
                + "interface to ietf-interfaces state", LOG));
    }

    private void processInterfaces(final vppInterfaceDetails[] ifaces) {
        for (vppInterfaceDetails swIf : ifaces) {
            interfaceDetails(swIf);
        }
    }

    /**
     * TODO-ADD-JAVADOC.
     */
    public void swInterfaceDumpAll() {
        vppInterfaceDetails[] ifaces;

        ifaces = api.swInterfaceDump((byte) 1, "Ether".getBytes());
        processInterfaces(ifaces);

        ifaces = api.swInterfaceDump((byte) 1, "lo".getBytes());
        processInterfaces(ifaces);

        ifaces = api.swInterfaceDump((byte) 1, "vxlan".getBytes());
        processInterfaces(ifaces);

        ifaces = api.swInterfaceDump((byte) 1, "l2tpv3_tunnel".getBytes());
        processInterfaces(ifaces);

        ifaces = api.swInterfaceDump((byte) 1, "tap".getBytes());
        processInterfaces(ifaces);
    }

    private void interfaceDetails(final vppInterfaceDetails swIf) {
        /*LOG.info("Got interface {} (idx: {}) adminUp: {} linkUp: {} duplex: {} speed: {} subId: {}",
         swIf.interfaceName, swIf.ifIndex, swIf.adminUp, swIf.linkUp, swIf.linkDuplex, swIf.linkSpeed, swIf.subId);*/

        vppInterfaceCounters ifCounters = api.getInterfaceCounters(swIf.ifIndex);

        InstanceIdentifier<Interface> iid = getStateInterfaceIid(swIf.interfaceName);

        Statistics stats = buildInterfaceStatistics(ifCounters);

        Interface intf = buildStateInterface(swIf.ifIndex, swIf.interfaceName,
                                             swIf.supIfIndex, swIf.physAddr,
                                             swIf.adminUp, swIf.linkUp,
                                             swIf.linkDuplex, swIf.linkSpeed,
                                             swIf.subId, swIf.subDot1ad,
                                             swIf.subNumberOfTags,
                                             swIf.subOuterVlanId,
                                             swIf.subInnerVlanId,
                                             swIf.subExactMatch, swIf.subDefault,
                                             swIf.subOuterVlanIdAny,
                                             swIf.subInnerVlanIdAny,
                                             swIf.vtrOp, swIf.vtrPushDot1q,
                                             swIf.vtrTag1, swIf.vtrTag2, stats);
        writeToIfState(iid, intf);

        ifNames += " " + swIf.interfaceName;
    }
}
