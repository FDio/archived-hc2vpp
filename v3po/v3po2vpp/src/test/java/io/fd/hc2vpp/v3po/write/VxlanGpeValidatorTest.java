/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.v3po.write;

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.DisabledInterfacesManager;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VxlanGpeNextProtocol;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VxlanGpeVni;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VxlanGpe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VxlanGpeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VxlanGpeValidatorTest {

    private VxlanGpeValidator validator;

    @Mock
    private WriteContext writeContext;
    @Mock
    private DisabledInterfacesManager interfaceDisableContext;

    private static final String IPV6 = "a::100";
    private static final String IPV4_1 = "192.168.20.10";
    private static final String IPV4_2 = "192.168.20.11";
    private static final Long VNI = Long.valueOf(11);
    private static final Long ENCAP = Long.valueOf(123);
    private static final Long DECAP = Long.valueOf(321);
    private static final String IFACE_NAME = "eth0";
    private static final InstanceIdentifier<VxlanGpe> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(VxlanGpe.class);

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new VxlanGpeValidator(ifcContext, interfaceDisableContext);
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
    public void testWriteFailedNoLocal() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(false, true, true, true, true, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoRemote() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(true, false, true, true, true, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoEncapVrfId() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(true, true, false, true, true, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoDecapVrfId() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(true, true, true, false, true, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoVNI() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(true, true, true, true, false, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoNextProtocol() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateVxlanSetFields(true, true, true, true, true, false), writeContext);
    }

    @Test
    public void testDeleteSuccessful() throws DeleteValidationFailedException {
        validator.validateDelete(ID, generateVxlanCorrect(), writeContext);
    }

    private VxlanGpe generateVxlanSetFields(final boolean src, final boolean dst, final boolean encapVrfId,
                                            final boolean decapVrfId, final boolean vni, final boolean protocol) {
        final VxlanGpeBuilder builder = new VxlanGpeBuilder();
        builder.setLocal(src
                ? new IpAddressNoZone(new Ipv4AddressNoZone(IPV4_1))
                : null);
        builder.setRemote(dst
                ? new IpAddressNoZone(new Ipv4AddressNoZone(IPV4_2))
                : null);
        builder.setEncapVrfId(encapVrfId
                ? ENCAP
                : null);
        builder.setDecapVrfId(decapVrfId
                ? DECAP
                : null);
        builder.setVni(vni
                ? new VxlanGpeVni(VNI)
                : null);
        builder.setNextProtocol(protocol
                ? VxlanGpeNextProtocol.forValue(1)
                : null);
        return builder.build();
    }

    private VxlanGpe generateVxlanCorrect() {
        return generateVxlanSetFields(true, true, true, true, true, true);
    }

    private VxlanGpe generateVxlanMixedIpFamilies() {
        return new VxlanGpeBuilder()
                .setLocal(new IpAddressNoZone(new Ipv6AddressNoZone(IPV6)))
                .setRemote(new IpAddressNoZone(new Ipv4AddressNoZone(IPV4_1)))
                .setEncapVrfId(ENCAP).setDecapVrfId(DECAP)
                .setVni(new VxlanGpeVni(VNI))
                .setNextProtocol(VxlanGpeNextProtocol.forValue(1))
                .build();
    }
}
