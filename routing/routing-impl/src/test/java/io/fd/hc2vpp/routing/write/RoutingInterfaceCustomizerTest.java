/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.routing.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class RoutingInterfaceCustomizerTest {
    private static final String IFC_NAME = "eth0";

    @Mock
    private WriteContext ctx;
    private RoutingInterfaceCustomizer customizer;

    @Before
    public final void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        customizer = new RoutingInterfaceCustomizer();

        when(ctx.readAfter(any())).thenReturn(Optional.absent());
        final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey>
            id = InstanceIdentifier.create(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces.class)
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class,
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey(
                    IFC_NAME));
        when(ctx.readAfter(id)).thenReturn(Optional.of(mock(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteIfcNotConfigured() throws WriteFailedException {
        final String nonExistingIfcName = "someIfc";
        final Interface ifc = mock(Interface.class);
        when(ifc.getName()).thenReturn(nonExistingIfcName);
        customizer.writeCurrentAttributes(getId(nonExistingIfcName), ifc, ctx);
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        final Interface ifc = mock(Interface.class);
        when(ifc.getName()).thenReturn(IFC_NAME);
        customizer.updateCurrentAttributes(getId(IFC_NAME), ifc, ifc, ctx);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        final Interface ifc = mock(Interface.class);
        when(ifc.getName()).thenReturn(IFC_NAME);
        customizer.deleteCurrentAttributes(getId(IFC_NAME), ifc, ctx);
        verifyZeroInteractions(ctx);
    }

    private InstanceIdentifier<Interface> getId(final String ifcName) {
        return InstanceIdentifier.create(Routing.class)
            .child(RoutingInstance.class, new RoutingInstanceKey("routingInstance")).child(Interfaces.class)
            .child(Interface.class, new InterfaceKey(ifcName));
    }
}