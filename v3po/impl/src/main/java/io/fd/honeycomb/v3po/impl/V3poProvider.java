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

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLength;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.V3poService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.EthernetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.l2.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V3poProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(V3poProvider.class);
    private RpcRegistration<V3poService> v3poService;
    private VppIetfInterfaceListener vppInterfaceListener;
    private VppBridgeDomainListener vppBridgeDomainListener;
    private vppApi api;
    private DataBroker db;
    VppPollOperDataImpl vppPollOperData;

    private void writeToBridgeDomain(final String bdName, final Boolean flood,
                                     final Boolean forward, final Boolean learn,
                                     final Boolean unknownUnicastFlood,
                                     final Boolean arpTermination) {

        BridgeDomainBuilder bdBuilder = new BridgeDomainBuilder();
        bdBuilder.setName(bdName);
        bdBuilder.setFlood(flood);
        bdBuilder.setForward(forward);
        bdBuilder.setLearn(learn);
        bdBuilder.setUnknownUnicastFlood(unknownUnicastFlood);
        bdBuilder.setArpTermination(arpTermination);

        LOG.info("VPPCFG-INFO: Adding Bridge Domain " + bdName + " to DataStore.");
        InstanceIdentifier<BridgeDomain> iid =
            InstanceIdentifier.create(Vpp.class)
            .child(BridgeDomains.class)
            .child(BridgeDomain.class, new BridgeDomainKey(bdName));
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, bdBuilder.build());
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<Void>(
                "VPPCFG-WARNING: Failed to write bridge domain " + bdName + " to Bridge Domains", LOG));
    }

    private void writeIpv4AddressToInterface(final String name, final Ipv4AddressNoZone ipv4Addr, final short plen) {
        AddressKey addrKey = new AddressKey(ipv4Addr);
        AddressBuilder addrBuilder = new AddressBuilder();
        PrefixLength prefixLen = new PrefixLengthBuilder().setPrefixLength(plen).build();
        addrBuilder.setSubnet(prefixLen);
        addrBuilder.setIp(new Ipv4AddressNoZone(ipv4Addr));
        addrBuilder.setKey(addrKey);

        List<Address> addrs = new ArrayList<Address>();
        addrs.add(addrBuilder.build());

        Ipv4 ip4 = new Ipv4Builder().setAddress(addrs).build();
        Interface1Builder if1Builder = new Interface1Builder();
        if1Builder.setIpv4(ip4);

        InterfaceBuilder ifBuilder = new InterfaceBuilder();
        ifBuilder.setName(name);
        ifBuilder.addAugmentation(Interface1.class, if1Builder.build());

        LOG.info("VPPCFG-INFO: Adding ipv4 address {} to interface {} to DataStore.", ipv4Addr, name);
        InstanceIdentifier<Interface> iid =
            InstanceIdentifier.create(Interfaces.class)
            .child(Interface.class, new InterfaceKey(name));
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, ifBuilder.build());
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<Void>(
                "VPPCFG-WARNING: Failed to write " + name + "interface to ietf-interfaces", LOG));
    }

    private void writeToInterface(final String name, final String description,
                                  final Boolean enabled, final String bdName, final int vrfId) {
        VppInterfaceAugmentationBuilder ifAugBuilder = new VppInterfaceAugmentationBuilder();

        EthernetBuilder ethBuilder = new EthernetBuilder();
        ethBuilder.setMtu(1234);
        ifAugBuilder.setEthernet(ethBuilder.build());

        if (bdName != null) {
            BridgeBasedBuilder bridgeBuilder = new BridgeBasedBuilder();
            bridgeBuilder.setBridgeDomain(bdName);
            bridgeBuilder.setSplitHorizonGroup((short)0);
            bridgeBuilder.setBridgedVirtualInterface(false);

            L2Builder l2Builder = new L2Builder();
            l2Builder.setInterconnection(bridgeBuilder.build());
            ifAugBuilder.setL2(l2Builder.build());
        }

        if (vrfId > 0) {
            RoutingBuilder rtBuilder = new RoutingBuilder();
            rtBuilder.setVrfId(new Long(vrfId));
            ifAugBuilder.setRouting(rtBuilder.build());
        }

        InterfaceBuilder ifBuilder = new InterfaceBuilder();
        ifBuilder.setName(name);
        ifBuilder.setDescription(description);
        ifBuilder.setType(EthernetCsmacd.class);
        ifBuilder.setEnabled(enabled);
        ifBuilder.setLinkUpDownTrapEnable(Interface.LinkUpDownTrapEnable.Disabled);

        ifBuilder.addAugmentation(VppInterfaceAugmentation.class, ifAugBuilder.build());

        LOG.info("VPPCFG-INFO: Adding interface " + name + " to DataStore.");
        InstanceIdentifier<Interface> iid = InstanceIdentifier.create(Interfaces.class)
            .child(Interface.class, new InterfaceKey(name));
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, ifBuilder.build());
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<Void>(
                "VPPCFG-WARNING: Failed to write " + name + "interface to ietf-interfaces", LOG));
    }

    private void initializeVppConfig() {

        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<Vpp> viid = InstanceIdentifier.create(Vpp.class);
        Vpp vpp = new VppBuilder().build();
        transaction.put(LogicalDatastoreType.CONFIGURATION, viid, vpp);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new
                            LoggingFuturesCallBack<>("VPPCFG-WARNING: Failed to create Vpp "
                                                     + "configuration db.",
                                                     LOG));
        vppBridgeDomainListener = new VppBridgeDomainListener(db, api);

        LOG.info("VPPCFG-INFO: Preparing to initialize the IETF Interface " + "list configuration db.");
        transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<Interfaces> iid = InstanceIdentifier.create(Interfaces.class);
        Interfaces intf = new InterfacesBuilder().build();
        transaction.put(LogicalDatastoreType.CONFIGURATION, iid, intf);
        future = transaction.submit();
        Futures.addCallback(future, new
                            LoggingFuturesCallBack<>("VPPCFG-WARNING: Failed to create IETF "
                                                     + "Interface list configuration db.",
                                                     LOG));
        vppInterfaceListener = new VppIetfInterfaceListener(db, api);

        /* DAW-DEBUG:
        try {
            int wait = 3;
            LOG.info("VPPCFG-INFO: Sleeping for {} seconds...", wait);
            TimeUnit.SECONDS.sleep(wait);
        } catch (InterruptedException e) {
            LOG.info("VPPCFG-INFO: Sleep Interrupted!");
        }
        LOG.info("VPPCFG-INFO: Nap complete.  I feel much better now.");
         */

        /* Test DataChangeListener by writing to db */
        writeToBridgeDomain("CocaCola", true /*flood*/, true /*forward*/,
                            true /*learn*/, true /*uuFlood*/,
                            false /*arpTermination*/);
        writeToBridgeDomain("PepsiCola", true /*flood*/, true /*forward*/,
                            true /*learn*/, true /*uuFlood*/,
                            false /*arpTermination*/);


        writeToInterface("TenGigabitEthernet86/0/1",
                         "Physical 10GbE Interface (Transport)",
                         true, null, 7);
        writeToInterface("TenGigabitEthernet86/0/0", "Physical 10GbE Interface",
                         true, "CocaCola", 0);
        writeToInterface("GigabitEthernet8/0/1", "Physical 1GbE Interface",
                         true, "PepsiCola", 0);

        /*
        writeIpv4AddressToInterface("GigabitEthernet86/0/1",
                                    new Ipv4AddressNoZone("10.10.10.10"),
                                    (short)24);
        writeIpv4AddressToInterface("GigabitEthernet86/0/1",
                                    new Ipv4AddressNoZone("11.11.11.10"),
                                    (short)24);
        writeIpv4AddressToInterface("GigabitEthernet86/0/1",
                                    new Ipv4AddressNoZone("11.11.11.10"),
                                    (short)24);
        */
        /* Interfaces on virtual testbed VMs (e.g. js-cluster-1) */
        writeToBridgeDomain("Balvenie", true /*flood*/, true /*forward*/,
                            true /*learn*/, true /*uuFlood*/,
                            false /*arpTermination*/);
        writeToBridgeDomain("Laphroaig", true /*flood*/, true /*forward*/,
                            true /*learn*/, true /*uuFlood*/,
                            false /*arpTermination*/);
        writeToBridgeDomain("Glenfiddich", true /*flood*/, true /*forward*/,
                            true /*learn*/, true /*uuFlood*/,
                            false /*arpTermination*/);
        writeToBridgeDomain("Macallan", true /*flood*/, true /*forward*/,
                            true /*learn*/, true /*uuFlood*/,
                            false /*arpTermination*/);

        writeToInterface("GigabitEthernet2/2/0", "Physical 1GbE Interface",
                         true, "Balvenie", 0);
        writeToInterface("GigabitEthernet2/3/0", "Physical 1GbE Interface",
                         true, "Laphroaig", 0);
        writeToInterface("GigabitEthernet2/4/0", "Physical 1GbE Interface",
                         true, "Glenfiddich", 0);
        writeToInterface("GigabitEthernet2/5/0", "Physical 1GbE Interface",
                         true, "Macallan", 0);
        writeToInterface("GigabitEthernet2/6/0",
                         "Physical 1GbE Interface (Transport)",
                         true, null, 7);
    }

    /* operational data */

    private void initVppOperational() {
        /*
         * List<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.
         * interfaces.rev140508.interfaces.state.Interface> ifaces = new
         * ArrayList<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.
         * ietf.interfaces.rev140508.interfaces.state.Interface>();
         */
        LOG.info("VPPOPER-INFO: Preparing to initialize the IETF Interface " + "state list operational db.");
        InterfacesState ifsState = new InterfacesStateBuilder().build();
        WriteTransaction tx = db.newWriteOnlyTransaction();
        InstanceIdentifier<InterfacesState> isid = InstanceIdentifier.builder(InterfacesState.class).build();
        tx.put(LogicalDatastoreType.OPERATIONAL, isid, ifsState);
        Futures.addCallback(tx.submit(), new LoggingFuturesCallBack<>(
                "VPPOPER-WARNING: Failed to create IETF " + "Interface state list operational db.", LOG));
    }

    private void startOperationalUpdateTimer() {
        Timer timer = new Timer();

        // fire task after 1 second and then repeat each 10 seconds
        timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    vppPollOperData.updateOperational();
                }
            }, 1000, 10000);
    }

    @Override
    public void onSessionInitiated(final ProviderContext session) {
        LOG.info("VPP-INFO: V3poProvider Session Initiated");

        try {
	    api = new vppApi("v3poODL");
        } catch (IOException e) {
            LOG.error("VPP-ERROR: VPP api client connection failed", e);
            return;
        }

        LOG.info("VPP-INFO: VPP api client connection established");

        db = session.getSALService(DataBroker.class);
        initializeVppConfig();
        initVppOperational();

        vppPollOperData = new VppPollOperDataImpl(api, db);
        v3poService = session.addRpcImplementation(V3poService.class,
                                                   vppPollOperData);
        startOperationalUpdateTimer();
    }

    @Override
    public void close() throws Exception {
        LOG.info("VPP-INFO: V3poProvider Closed");
        if (v3poService != null) {
            v3poService.close();
        }
        if (api != null) {
            api.close();
        }
    }
}
