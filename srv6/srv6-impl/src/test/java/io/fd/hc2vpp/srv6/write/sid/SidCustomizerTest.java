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

package io.fd.hc2vpp.srv6.write.sid;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.hc2vpp.srv6.write.sid.request.LocalSidRequestTest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.locator.Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.locator.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.End;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6LocatorLen;

public class SidCustomizerTest extends LocalSidRequestTest {

    private static final Ipv6Address IP_ADDRESS = new Ipv6Address("a::101");

    @Before
    public void setup() {
        when(ctx.readAfter(any())).thenReturn(Optional.of(LOCATOR));
        when(ctx.readBefore(any())).thenReturn(Optional.of(LOCATOR));
    }

    @Test(expected = NullPointerException.class)
    public void writeCurrentAttributesNullTest() throws WriteFailedException {
        SidCustomizer customizer = new SidCustomizer(api, WRITE_REGISTRY);
        Sid localSid = getSidNull();
        customizer.writeCurrentAttributes(SID_A_101, localSid, ctx);
        verify(api, times(0)).srLocalsidAddDel(any());
    }

    @Test(expected = NullPointerException.class)
    public void deleteCurrentAttributesNullTest() throws WriteFailedException {
        SidCustomizer customizer = new SidCustomizer(api, WRITE_REGISTRY);
        Sid localSid = getSidNull();
        customizer.deleteCurrentAttributes(SID_A_101, localSid, ctx);
        verify(api, times(0)).srLocalsidAddDel(any());
    }

    @Test
    public void resolveSidAddressTest() {
        Sid localSid = getSidNull();
        SidCustomizer customizer = new SidCustomizer(api, WRITE_REGISTRY);
        Prefix locPrefix = new PrefixBuilder().setAddress(new Ipv6Address("a::")).setLength(new Srv6LocatorLen(
                (short) 64)).build();
        Ipv6Address ipv6Address = customizer.resolveSidAddress(locPrefix, localSid);
        assertTrue((IP_ADDRESS.equals(ipv6Address)));
    }

    private Sid getSidNull() {
        return new SidBuilder()
                .setOpcode(OPCODE_A_101)
                .setEndBehaviorType(End.class)
                .setEnd(null)
                .build();
    }
}
