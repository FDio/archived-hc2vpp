/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.write.sid.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.srv6.Srv6IIds;
import io.fd.hc2vpp.srv6.util.JvppRequestTest;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.jvpp.core.dto.SrLocalsidAddDel;
import io.fd.jvpp.core.dto.SrLocalsidAddDelReply;
import java.util.Arrays;
import org.junit.Assert;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.Locator1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator.Static;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.routing.srv6.locators.locator._static.LocalSids;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.locator.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6FuncOpcodeUnreserved;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6LocatorLen;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class LocalSidRequestTest extends JvppRequestTest {

    protected static final Srv6FuncOpcodeUnreserved OPCODE_A_101 =
            new Srv6FuncOpcodeUnreserved(257L); // 101 in hex format for IPv6
    protected static final InstanceIdentifier<Sid> SID_A_101 =
            Srv6IIds.RT_SRV6_LOCATORS.child(Locator.class, new LocatorKey("a::")).augmentation(Locator1.class)
                    .child(Static.class).child(LocalSids.class).child(Sid.class, new SidKey(OPCODE_A_101));
    static final byte[] SID_BYTES = {0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    static final Ipv6Address SID = new Ipv6Address("A::0");
    protected static Locator LOCATOR = new LocatorBuilder().setName("a::").setPrefix(
            new PrefixBuilder().setLength(new Srv6LocatorLen((short) 64)).setAddress(new Ipv6Address("a::")).build())
            .build();

    @Mock
    protected static LocatorContextManager locatorContext;

    @Captor
    ArgumentCaptor<SrLocalsidAddDel> requestcaptor;

    static void assertIsCreate(final SrLocalsidAddDel request) {
        assertEquals(0, request.isDel);
    }

    static void assertIsDelete(final SrLocalsidAddDel request) {
        assertEquals(1, request.isDel);
    }

    static void assertBaseFields(final SrLocalsidAddDel request, final byte[] localSidAddress,
                                 final int installFibTable, final int segmentRoutingBehavior,
                                 final int isDecapsulationEnabled) {
        assertEquals(installFibTable, request.fibTable);
        assertEquals(isDecapsulationEnabled, request.endPsp);
        assertEquals(segmentRoutingBehavior, request.behavior);
        assertTrue(Arrays.equals(localSidAddress, request.localsid.addr));
    }

    public static void assertRequestEqual(SrLocalsidAddDel srLocalsidAddDel, int behaviour, boolean isWrite,
                                          String localSid) {
        //PSP is true whe USP is false and vice versa
        Assert.assertFalse(!ByteDataTranslator.INSTANCE.byteToBoolean(srLocalsidAddDel.endPsp));
        Assert.assertEquals(behaviour, (int) srLocalsidAddDel.behavior);
        Assert.assertEquals(isWrite, !ByteDataTranslator.INSTANCE.byteToBoolean(srLocalsidAddDel.isDel));
        IpAddress ipAddress = AddressTranslator.INSTANCE.arrayToIpAddress(true, srLocalsidAddDel.localsid.addr);
        Assert.assertEquals(localSid.toLowerCase(), ipAddress.getIpv6Address().getValue().toLowerCase());
    }

    @Override
    protected void init() {
        when(api.srLocalsidAddDel(any())).thenReturn(future(new SrLocalsidAddDelReply()));
    }
}
