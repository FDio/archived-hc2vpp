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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip4.attributes.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip6.attributes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.routing.attributes.Routing;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.routing.attributes.RoutingBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceRoutingValidatorTest {

    private SubInterfaceRoutingValidator validator;

    @Mock
    private WriteContext writeContext;

    private static final String IF_NAME = "eth1";
    private static final int SUBIF_INDEX = 0;
    private static final InstanceIdentifier<Routing> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .augmentation(SubinterfaceAugmentation.class)
                    .child(SubInterfaces.class)
                    .child(SubInterface.class, new SubInterfaceKey((long) SUBIF_INDEX))
                    .child(Routing.class);

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new SubInterfaceRoutingValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful() throws CreateValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.empty());
        validator.validateWrite(ID, getRouting(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedV4AddressPresent() throws CreateValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.of(v4AddressPresent()));
        validator.validateWrite(ID, getRouting(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedV6AddressPresent() throws CreateValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.of(v6AddressPresent()));
        validator.validateWrite(ID, getRouting(), writeContext);
    }

    @Test
    public void testUpdateSuccessful() throws UpdateValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.empty());
        validator.validateUpdate(ID, getRouting(), getRouting(), writeContext);
    }

    @Test
    public void testDeleteSuccessful() throws DeleteValidationFailedException {
        when(writeContext.readBefore(any(InstanceIdentifier.class))).thenReturn(Optional.empty());
        validator.validateDelete(ID, getRouting(), writeContext);
    }

    private Routing getRouting() {
        return new RoutingBuilder().setIpv4VrfId(new VniReference(4L)).build();
    }

    private SubInterface v4AddressPresent() {
        return new SubInterfaceBuilder()
                .setIpv4(new Ipv4Builder()
                        .setAddress(Collections.singletonList(new AddressBuilder().build()))
                        .build())
                .build();
    }

    private SubInterface v6AddressPresent() {
        return new SubInterfaceBuilder()
                .setIpv6(new Ipv6Builder()
                        .setAddress(Collections.singletonList(
                                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.sub._interface.ip6.attributes.ipv6.AddressBuilder()
                                        .build()))
                        .build())
                .build();
    }
}
