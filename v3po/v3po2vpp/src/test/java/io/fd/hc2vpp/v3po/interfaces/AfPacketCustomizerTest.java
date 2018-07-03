/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.v3po.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.AfPacketCreate;
import io.fd.vpp.jvpp.core.dto.AfPacketCreateReply;
import io.fd.vpp.jvpp.core.dto.AfPacketDelete;
import io.fd.vpp.jvpp.core.dto.AfPacketDeleteReply;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.interfaces._interface.AfPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.interfaces._interface.AfPacketBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AfPacketCustomizerTest extends WriterCustomizerTest implements Ipv4Translator {

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final int IFACE_ID = 1;
    private static final String IFACE_NAME = "veth1";
    private static final InstanceIdentifier<AfPacket> ID =
        InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
            .augmentation(VppInterfaceAugmentation.class).child(AfPacket.class);
    private AfPacketCustomizer customizer;

    @Override
    public void setUpTest() throws Exception {
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev180703.AfPacket.class);
        customizer = new AfPacketCustomizer(api, new NamingContext("generatedInterfaceName", IFC_TEST_INSTANCE));

        final AfPacketCreateReply createReply = new AfPacketCreateReply();
        createReply.swIfIndex = IFACE_ID;
        when(api.afPacketCreate(any())).thenReturn(future(createReply));
        when(api.afPacketDelete(any())).thenReturn(future(new AfPacketDeleteReply()));
    }

    @Test
    public void testWriteRandomMac() throws WriteFailedException {
        final AfPacketCreate expectedCreateRequest = new AfPacketCreate();
        expectedCreateRequest.hostIfName = IFACE_NAME.getBytes(StandardCharsets.UTF_8);
        expectedCreateRequest.useRandomHwAddr = 1;
        expectedCreateRequest.hwAddr = new byte[6];

        customizer.writeCurrentAttributes(ID, afPacket(), writeContext);

        verify(api).afPacketCreate(expectedCreateRequest);
        verify(mappingContext).put(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE), mapping(IFACE_NAME, IFACE_ID).get());
    }

    @Test
    public void testWriteExplicitMac() throws WriteFailedException {
        final AfPacket afPacket = afPacket("01:02:03:04:05:06");

        final AfPacketCreate expectedCreateRequest = new AfPacketCreate();
        expectedCreateRequest.hostIfName = IFACE_NAME.getBytes(StandardCharsets.UTF_8);
        expectedCreateRequest.useRandomHwAddr = 0;
        expectedCreateRequest.hwAddr = new byte[] {1, 2, 3, 4, 5, 6};

        customizer.writeCurrentAttributes(ID, afPacket, writeContext);

        verify(api).afPacketCreate(expectedCreateRequest);
        verify(mappingContext).put(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE), mapping(IFACE_NAME, IFACE_ID).get());
    }

    @Test
    public void testDelete() throws WriteFailedException {
        final AfPacket afPacket = afPacket("02:03:04:05:06:07");
        final AfPacketDelete expectedDeleteRequest = new AfPacketDelete();
        expectedDeleteRequest.hostIfName = IFACE_NAME.getBytes(StandardCharsets.UTF_8);

        customizer.deleteCurrentAttributes(ID, afPacket, writeContext);

        verify(api).afPacketDelete(expectedDeleteRequest);
        verify(mappingContext).delete(eq(mappingIid(IFACE_NAME, IFC_TEST_INSTANCE)));
    }

    private static AfPacket afPacket() {
        return new AfPacketBuilder().setHostInterfaceName(IFACE_NAME).build();
    }

    private static AfPacket afPacket(String mac) {
        return new AfPacketBuilder().setHostInterfaceName(IFACE_NAME).setMac(new PhysAddress(mac)).build();
    }
}
