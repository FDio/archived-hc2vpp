/*
 * Copyright (c) 2019 PANTHEON.tech.
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

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Gre;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.GreBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GreValidatorTest {

    private GreValidator validator;

    @Mock
    private WriteContext writeContext;

    private static final String IPV6 = "a::100";
    private static final String IPV4_1 = "192.168.20.10";
    private static final String IPV4_2 = "192.168.20.11";
    private static final Long OUT_FIB_ID = Long.valueOf(123);
    private static final String IFACE_NAME = "eth0";
    private static final InstanceIdentifier<Gre> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(Gre.class);

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new GreValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateCorrectGre(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedMixedIpv4Ipv6() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateGre(ip4(IPV4_1), ip6(IPV6), OUT_FIB_ID), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoSrcAddr() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateGre(null, ip6(IPV6), OUT_FIB_ID), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoDstAddr() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateGre(ip4(IPV4_1), null, OUT_FIB_ID), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoOutFibId() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateGre(ip4(IPV4_1), ip4(IPV4_2), null), writeContext);
    }

    @Test
    public void testDeleteSuccessful() throws DeleteValidationFailedException {
        validator.validateDelete(ID, generateCorrectGre(), writeContext);
    }

    private Gre generateCorrectGre() {
        return generateGre(ip4(IPV4_1), ip4(IPV4_2), OUT_FIB_ID);
    }

    private Gre generateGre(final IpAddressNoZone srcAddr, final IpAddressNoZone dstAddr, final Long outerFibId) {
        final GreBuilder builder = new GreBuilder();
        builder.setSrc(srcAddr);
        builder.setDst(dstAddr);
        builder.setOuterFibId(outerFibId);
        return builder.build();
    }

    private IpAddressNoZone ip4(String addr) {
        return new IpAddressNoZone(new Ipv4AddressNoZone(addr));
    }

    private IpAddressNoZone ip6(String addr) {
        return new IpAddressNoZone(new Ipv6AddressNoZone(addr));
    }
}
