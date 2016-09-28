/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.honeycomb.translate.v3po.interfaces;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.XconnectBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.base.attributes.interconnection.XconnectBasedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2Bridge;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2BridgeReply;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2Xconnect;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetL2XconnectReply;

public class L2CustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {
    private static final String IFACE_CTX_NAME = "interface-ctx";
    private static final String BD_CTX_NAME = "bd-ctx";
    private static final String IF1_NAME = "eth1";
    private static final int IF1_INDEX = 1;
    private static final String IF2_NAME = "eth2";
    private static final int IF2_INDEX = 2;
    private static final InstanceIdentifier<L2> IID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF1_NAME))
            .augmentation(VppInterfaceAugmentation.class).child(L2.class);
    private static final String BD_NAME = "test_bd";
    private static final int BD_INDEX = 13;

    private L2Customizer customizer;

    @Override
    protected void setUp() throws Exception {
        customizer = new L2Customizer(api, new NamingContext("ifacePrefix", IFACE_CTX_NAME),
            new NamingContext("bdPrefix", BD_CTX_NAME));
        defineMapping(mappingContext, IF1_NAME, IF1_INDEX, IFACE_CTX_NAME);
        defineMapping(mappingContext, IF2_NAME, IF2_INDEX, IFACE_CTX_NAME);
        defineMapping(mappingContext, BD_NAME, BD_INDEX, BD_CTX_NAME);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(api.swInterfaceSetL2Xconnect(any())).thenReturn(future(new SwInterfaceSetL2XconnectReply()));
        customizer.writeCurrentAttributes(IID, l2(xconnectBased()), writeContext);
        verify(api).swInterfaceSetL2Xconnect(xconnectRequest(true));
    }

    @Test
    public void testWriteFailed() {
        when(api.swInterfaceSetL2Bridge(any())).thenReturn(failedFuture());
        try {
            customizer.writeCurrentAttributes(IID, l2(bridgeBased(false)), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceSetL2Bridge(bridgeRequest(false, true));
            return;
        }
        fail("WriteFailedException expected");
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        when(api.swInterfaceSetL2Bridge(any())).thenReturn(future(new SwInterfaceSetL2BridgeReply()));
        customizer.updateCurrentAttributes(IID, l2(bridgeBased(false)), l2(bridgeBased(true)), writeContext);
        verify(api).swInterfaceSetL2Bridge(bridgeRequest(true, true));
    }

    @Test
    public void testDelete() throws WriteFailedException {
        when(api.swInterfaceSetL2Xconnect(any())).thenReturn(future(new SwInterfaceSetL2XconnectReply()));
        customizer.deleteCurrentAttributes(IID, l2(xconnectBased()), writeContext);
        verify(api).swInterfaceSetL2Xconnect(xconnectRequest(false));
    }

    @Test
    public void testDeleteFailed() {
        when(api.swInterfaceSetL2Bridge(any())).thenReturn(failedFuture());
        try {
            customizer.deleteCurrentAttributes(IID, l2(bridgeBased(true)), writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(api).swInterfaceSetL2Bridge(bridgeRequest(true, false));
            return;
        }
        fail("WriteFailedException expected");
    }

    private XconnectBased xconnectBased() {
        return new XconnectBasedBuilder().setXconnectOutgoingInterface(IF2_NAME).build();
    }

    private SwInterfaceSetL2Xconnect xconnectRequest(final boolean enable) {
        final SwInterfaceSetL2Xconnect request = new SwInterfaceSetL2Xconnect();
        request.rxSwIfIndex = IF1_INDEX;
        request.txSwIfIndex = IF2_INDEX;
        request.enable = booleanToByte(enable);
        return request;
    }

    private BridgeBased bridgeBased(final boolean bvi) {
        return new BridgeBasedBuilder().setBridgedVirtualInterface(bvi)
            .setBridgeDomain(BD_NAME).setSplitHorizonGroup((short) 123).build();
    }

    private SwInterfaceSetL2Bridge bridgeRequest(final boolean bvi, final boolean enable) {
        final SwInterfaceSetL2Bridge request = new SwInterfaceSetL2Bridge();
        request.bdId = BD_INDEX;
        request.rxSwIfIndex = IF1_INDEX;
        request.bvi = booleanToByte(bvi);
        request.enable = booleanToByte(enable);
        request.shg = 123;
        return request;
    }


    private L2 l2(final Interconnection interconnection) {
        return new L2Builder().setInterconnection(interconnection).build();
    }
}