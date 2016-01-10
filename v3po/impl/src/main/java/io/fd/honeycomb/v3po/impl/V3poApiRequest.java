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

import java.math.BigInteger;
import java.net.InetAddress;

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
import org.openvpp.vppjapi.vppBridgeDomainDetails;
import org.openvpp.vppjapi.vppBridgeDomainInterfaceDetails;
import org.openvpp.vppjapi.vppIPv4Address;
import org.openvpp.vppjapi.vppIPv6Address;
import org.openvpp.vppjapi.vppInterfaceCounters;
import org.openvpp.vppjapi.vppInterfaceDetails;
import org.openvpp.vppjapi.vppVxlanTunnelDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
/*
 * VPP API Class overriding interface details callback
 */
public class V3poApiRequest extends V3poRequest {
    private static final Logger LOG = LoggerFactory.getLogger(V3poApiRequest.class);
    public String ifNames = "";
    private VppPollOperDataImpl caller;

    public V3poApiRequest(VppPollOperDataImpl vppPollOperData) {
        caller = vppPollOperData;
    }

    private InstanceIdentifier<Interface> getStateInterfaceIid(String interfaceName) {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class,
                new InterfaceKey(interfaceName));
    }
    
    private InstanceIdentifier<Interface2> getStateInterfaceIpId(InstanceIdentifier<Interface> iid) {
        return iid.augmentation(Interface2.class);
    }
    
    private InstanceIdentifier<Statistics> getStateInterfaceStatsId(InstanceIdentifier<Interface> iid) {
        return iid.child(Statistics.class);
    }
    
    private static Counter64 getCounter64(long num) {
        return new Counter64(BigInteger.valueOf(num));
    }
    
    private static Counter32 getCounter32(long num) {
        return new Counter32(num);
    }
    
    private Statistics buildInterfaceStatistics(vppInterfaceCounters ifCounters) {
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

    private static String getMacAddress(byte[] mac) {
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

    private static Gauge64 getSpeed(byte vppSpeed) {
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
    
    private static String ipv4IntToString(int ip) {
        InetAddress addr = null;
        byte[] bytes = Ints.toByteArray(ip);
        try {
            addr = InetAddress.getByAddress(bytes);
        } catch (java.net.UnknownHostException e) {
            e.printStackTrace();
            return null;
        }
        return addr.getHostAddress();
    }

    private Interface buildStateInterface(int ifIndex,
                                          String interfaceName,
                                          int supIfIndex,
                                          byte[] physAddr,
                                          byte adminUp, byte linkUp,
                                          byte linkDuplex, byte linkSpeed,
                                          int subId, byte subDot1ad,
                                          byte subNumberOfTags,
                                          int subOuterVlanId,
                                          int subInnerVlanId,
                                          byte subExactMatch,
                                          byte subDefault,
                                          byte subOuterVlanIdAny,
                                          byte subInnerVlanIdAny,
                                          int vtrOp, int vtrPushDot1q,
                                          int vtrTag1, int vtrTag2,
                                          Statistics stats) {
        InterfaceBuilder ifBuilder = new InterfaceBuilder();
        java.lang.Class<? extends InterfaceType> ifType;
        
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
        int bdId = this.bridgeDomainIdFromInterfaceName(interfaceName);
        vppBridgeDomainDetails bd = (bdId != -1 ? this.getBridgeDomainDetails(bdId) : null);
        
        String bdName = null;
        short splitHorizonGroup = 0;
        boolean bvi = false;
        
        if (bd != null) {
            bdName = bd.name;
            for (int ifIdx = 0; ifIdx < bd.interfaces.length; ifIdx++) {
                vppBridgeDomainInterfaceDetails bdIf = bd.interfaces[ifIdx];
                
                if (bdIf.interfaceName != interfaceName) {
                    continue;
                }
                if (bd.bviInterfaceName == interfaceName) {
                    bvi = true;
                }
                splitHorizonGroup = (short)bdIf.splitHorizonGroup;
            }
        }

        VppInterfaceStateAugmentationBuilder vppIfStateAugBuilder =
            new VppInterfaceStateAugmentationBuilder();

        vppIfStateAugBuilder.setDescription(this.getInterfaceDescription(interfaceName));

        setStateInterfaceL2(vppIfStateAugBuilder, bdId != -1, false, null,
                            bdName, splitHorizonGroup, bvi);

        if (EthernetCsmacd.class == ifType) {
            setStateInterfaceEthernet(vppIfStateAugBuilder, linkDuplex == 2,
                                      "ACME Inc.", 1234);
        }

        vppVxlanTunnelDetails[] vxlanDet = this.vxlanTunnelDump(ifIndex);
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
        
        vppIPv4Address[] ipv4Addrs = ipv4AddressDump(interfaceName);
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
        
        vppIPv6Address[] ipv6Addrs = ipv6AddressDump(interfaceName);
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
    
    private void setStateInterfaceL2(
            VppInterfaceStateAugmentationBuilder augBuilder,
            boolean isL2BridgeBased, boolean isXconnect,
            String xconnectOutgoingInterface,
            String bdName, short splitHorizonGroup, boolean bvi) {

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
    
    private void setStateInterfaceEthernet(
            VppInterfaceStateAugmentationBuilder augBuilder,
            boolean isFullDuplex, String manufacturerDesc, Integer mtu) {

        EthernetBuilder ethBuilder = new EthernetBuilder();
        ethBuilder.setDuplex((isFullDuplex ? Duplex.Full : Duplex.Half))
            .setManufacturerDescription(manufacturerDesc)
            .setMtu(mtu);

        augBuilder.setEthernet(ethBuilder.build());
    }

    private void setStateInterfaceVxlan(
            VppInterfaceStateAugmentationBuilder augBuilder, int srcAddress,
            int dstAddress, int vni, int encapVrfId) {

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

    private void writeToIfState(InstanceIdentifier<Interface> iid,
                                Interface intf) {
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
    
    private void processInterfaces(vppInterfaceDetails[] ifaces) {
        for (vppInterfaceDetails swIf : ifaces) {
            interfaceDetails(swIf);
        }
    }
    
    /**
     * TODO-ADD-JAVADOC.
     */
    public void swInterfaceDumpAll() {
        vppInterfaceDetails[] ifaces;
        
        ifaces = swInterfaceDump((byte) 1, "Ether".getBytes());
        processInterfaces(ifaces);
        
        ifaces = swInterfaceDump((byte) 1, "lo".getBytes());
        processInterfaces(ifaces);
        
        ifaces = swInterfaceDump((byte) 1, "vxlan".getBytes());
        processInterfaces(ifaces);
        
        ifaces = swInterfaceDump((byte) 1, "l2tpv3_tunnel".getBytes());
        processInterfaces(ifaces);
        
        ifaces = swInterfaceDump((byte) 1, "tap".getBytes());
        processInterfaces(ifaces);
    }

    private void interfaceDetails(vppInterfaceDetails swIf) {
        /*LOG.info("Got interface {} (idx: {}) adminUp: {} linkUp: {} duplex: {} speed: {} subId: {}",
         swIf.interfaceName, swIf.ifIndex, swIf.adminUp, swIf.linkUp, swIf.linkDuplex, swIf.linkSpeed, swIf.subId);*/

        vppInterfaceCounters ifCounters = getInterfaceCounters(swIf.ifIndex);

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
