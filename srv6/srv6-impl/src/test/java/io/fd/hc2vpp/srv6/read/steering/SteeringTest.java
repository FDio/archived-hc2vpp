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


package io.fd.hc2vpp.srv6.read.steering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.srv6.write.sid.request.LocalSidRequestTest;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.jvpp.core.dto.SrSteeringPolDetails;
import io.fd.jvpp.core.dto.SrSteeringPolDetailsReplyDump;
import io.fd.jvpp.core.types.Srv6Sid;
import java.util.ArrayList;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

public abstract class SteeringTest extends LocalSidRequestTest {
    private static final Ipv6Address A = new Ipv6Address("a::");
    private static final Ipv4Address B = new Ipv4Address("10.0.0.1");
    static final Ipv6Address BSID_ADR = new Ipv6Address("a::e");

    @Mock
    ReadContext readCtx;

    @Mock
    ModificationCache modificationCache;

    SrSteeringPolDetailsReplyDump replyDump = new SrSteeringPolDetailsReplyDump();

    @Override
    protected void init() {
        when(readCtx.getModificationCache()).thenReturn(modificationCache);
        when(modificationCache.get(any())).thenReturn(replyDump);
        replyDump.srSteeringPolDetails = new ArrayList<>();
        SrSteeringPolDetails polDetailsA = new SrSteeringPolDetails();
        polDetailsA.trafficType = 6;
        polDetailsA.fibTable = 1;
        polDetailsA.prefixAddr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(A));
        Srv6Sid bsidA = new Srv6Sid();
        bsidA.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_ADR));
        polDetailsA.bsid = bsidA;
        polDetailsA.maskWidth = 64;

        SrSteeringPolDetails polDetailsB = new SrSteeringPolDetails();
        polDetailsB.trafficType = 4;
        polDetailsB.fibTable = 0;
        polDetailsB.prefixAddr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(B));
        Srv6Sid bsidB = new Srv6Sid();
        bsidB.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_ADR));
        polDetailsB.bsid = bsidB;
        polDetailsB.maskWidth = 24;

        SrSteeringPolDetails polDetailsC = new SrSteeringPolDetails();
        polDetailsC.trafficType = 2;
        polDetailsC.swIfIndex = 1;
        bsidA.addr = AddressTranslator.INSTANCE.ipAddressToArray(new IpAddress(BSID_ADR));
        polDetailsA.bsid = bsidA;

        replyDump.srSteeringPolDetails.add(polDetailsA);
        replyDump.srSteeringPolDetails.add(polDetailsB);
        replyDump.srSteeringPolDetails.add(polDetailsC);
    }
}
