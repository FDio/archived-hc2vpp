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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Routing;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.RoutingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceRoutingValidatorTest {

    private InterfaceRoutingValidator validator;

    @Mock
    private WriteContext writeContext;

    private static final String IF_NAME = "eth1";
    private static final Long VRF_ID = Long.valueOf(123);

    private static final InstanceIdentifier<Routing> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(Routing.class);

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new InterfaceRoutingValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful() throws CreateValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.empty());
        validator.validateWrite(ID, routing(VRF_ID, true, false), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoFrfIds() throws CreateValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.empty());
        validator.validateWrite(ID, routing(null, true, true), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedWithV4Address() throws CreateValidationFailedException {
        when(writeContext.readBefore(RWUtils.cutId(ID, Interface.class)))
                .thenReturn(Optional.of(ifaceWithV4Address()));
        validator.validateWrite(ID, routing(VRF_ID, true, false), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedWithV6Address() throws CreateValidationFailedException {
        when(writeContext.readBefore(RWUtils.cutId(ID, Interface.class)))
                .thenReturn(Optional.of(ifaceWithV6Address()));
        validator.validateWrite(ID, routing(VRF_ID, true, false), writeContext);
    }

    @Test
    public void testUpdateSuccessful() throws UpdateValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.empty());
        validator.validateUpdate(ID, routing(VRF_ID, true, false),
                routing(VRF_ID, true, true), writeContext);
    }

    @Test
    public void testDeleteSuccessful() throws DeleteValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.empty());
        validator.validateDelete(ID, routing(VRF_ID, true, false), writeContext);
    }

    private Routing routing(final Long vrfId, final boolean hasIpv4, final boolean hasIpv6) {
        VniReference vni = null;
        if (vrfId != null) {
            vni = new VniReference(vrfId);
        }

        RoutingBuilder builder = new RoutingBuilder();
        if (hasIpv4) {
            builder.setIpv4VrfId(vni);
        }
        if (hasIpv6) {
            builder.setIpv6VrfId(vni);
        }
        return builder.build();
    }

    private Interface ifaceWithV4Address() {
        return new InterfaceBuilder()
                .addAugmentation(Interface1.class, new Interface1Builder()
                        .setIpv4(new Ipv4Builder()
                                .setAddress(Collections.singletonList(new AddressBuilder().build()))
                                .build())
                        .build())
                .build();
    }

    private Interface ifaceWithV6Address() {
        return new InterfaceBuilder()
                .addAugmentation(Interface1.class, new Interface1Builder()
                        .setIpv6(new Ipv6Builder()
                                .setAddress(Collections.singletonList(
                                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv6.AddressBuilder()
                                                .build()))
                                .build())
                        .build())
                .build();
    }
}
