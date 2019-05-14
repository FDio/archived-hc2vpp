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
import io.fd.hc2vpp.v3po.DisabledInterfacesManager;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.L2Input;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VxlanVni;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Vxlan;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.VxlanBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VxlanValidatorTest {

    private VxlanValidator validator;

    @Mock
    private WriteContext writeContext;
    @Mock
    private DisabledInterfacesManager interfaceDisableContext;

    private static final String IPV6 = "a::100";
    private static final String IPV4_1 = "192.168.20.10";
    private static final String IPV4_2 = "192.168.20.11";
    private static final Long VNI = Long.valueOf(11);
    private static final Long ENCAP = Long.valueOf(123);
    private static final String IFACE_NAME = "eth0";
    private static final InstanceIdentifier<Vxlan> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(Vxlan.class);

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new VxlanValidator(ifcContext, interfaceDisableContext);
    }

    @Test
    public void testWriteSuccessful() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanCorrect(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedMixedIpFamilies() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanMixedIpFamilies(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoSrc() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(false, true, true, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoDst() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(true, false, true, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoEncap() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(true, true, false, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoVNI() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(true, true, true, false), writeContext);
    }

    @Test
    public void testDeleteSuccessful() throws DeleteValidationFailedException {
        validator.validateDelete(ID, generateVxlanCorrect(), writeContext);
    }

    private Vxlan generateVxlan(final IpAddressNoZone src, final IpAddressNoZone dst, final VniReference encapVrfId,
                                final long vni) {
        final VxlanBuilder builder = new VxlanBuilder();
        builder.setSrc(src);
        builder.setDst(dst);
        builder.setEncapVrfId(encapVrfId);
        builder.setVni(new VxlanVni(vni));
        builder.setDecapNext(L2Input.class);
        return builder.build();
    }

    private Vxlan generateVxlanSetFields(final boolean src, final boolean dst, final boolean encapVrfId,
                                         final boolean vni) {
        final VxlanBuilder builder = new VxlanBuilder();
        builder.setSrc(src
                ? new IpAddressNoZone(new Ipv4AddressNoZone(IPV4_1))
                : null);
        builder.setDst(dst
                ? new IpAddressNoZone(new Ipv4AddressNoZone(IPV4_2))
                : null);
        builder.setEncapVrfId(encapVrfId
                ? new VniReference(ENCAP)
                : null);
        builder.setVni(vni
                ? new VxlanVni(VNI)
                : null);
        builder.setDecapNext(L2Input.class);
        return builder.build();
    }

    private Vxlan generateVxlanCorrect() {
        return generateVxlanSetFields(true, true, true, true);
    }

    private Vxlan generateVxlanMixedIpFamilies() {
        return generateVxlan(new IpAddressNoZone(new Ipv6AddressNoZone(IPV6)),
                new IpAddressNoZone(new Ipv4AddressNoZone(IPV4_2)), new VniReference(ENCAP), VNI);
    }
}
