/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.nat.read.ifc;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.snat.dto.Nat64InterfaceDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetails;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceOutputFeatureDetails;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceOutputFeatureDetailsReplyDump;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816.NatInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170816._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceInboundNatCustomizerTest
        extends ReaderCustomizerTest<Inbound, InboundBuilder> {

    private static final String IFC_NAME = "a";
    private static final int IFC_IDX = 0;
    private static final String CTX_NAME = "ifc";

    @Mock
    private FutureJVppSnatFacade jvppSnat;

    private NamingContext ifcContext = new NamingContext(CTX_NAME, CTX_NAME);
    private InstanceIdentifier<Inbound> id;

    public InterfaceInboundNatCustomizerTest() {
        super(Inbound.class, NatBuilder.class);
    }

    static <T extends ChildOf<Nat>> InstanceIdentifier<T> getId(Class<T> boundType) {
        return InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(IFC_NAME))
                .augmentation(NatInterfaceStateAugmentation.class)
                .child(Nat.class)
                .child(boundType);
    }

    @Override
    protected void setUp() throws Exception {
        id = getId(Inbound.class);
        defineMapping(mappingContext, IFC_NAME, IFC_IDX, CTX_NAME);
        when(jvppSnat.snatInterfaceDump(any())).thenReturn(future(new SnatInterfaceDetailsReplyDump()));
        when(jvppSnat.snatInterfaceOutputFeatureDump(any()))
                .thenReturn(future(new SnatInterfaceOutputFeatureDetailsReplyDump()));
        when(jvppSnat.nat64InterfaceDump(any()))
                .thenReturn(future(new Nat64InterfaceDetailsReplyDump()));
    }

    private GenericReader<Inbound, InboundBuilder> getReader() {
        return new GenericReader<>(RWUtils.makeIidWildcarded(id), customizer);
    }

    private void mockPostRoutingDump() {
        final SnatInterfaceOutputFeatureDetailsReplyDump details = new SnatInterfaceOutputFeatureDetailsReplyDump();
        final SnatInterfaceOutputFeatureDetails detail = new SnatInterfaceOutputFeatureDetails();
        detail.isInside = 1;
        detail.swIfIndex = IFC_IDX;
        details.snatInterfaceOutputFeatureDetails = Lists.newArrayList(detail);
        when(jvppSnat.snatInterfaceOutputFeatureDump(any())).thenReturn(future(details));
    }

    @Test
    public void testPresencePreRouting() throws Exception {
        final SnatInterfaceDetailsReplyDump details = new SnatInterfaceDetailsReplyDump();
        final SnatInterfaceDetails detail = new SnatInterfaceDetails();
        detail.isInside = 1;
        detail.swIfIndex = IFC_IDX;
        details.snatInterfaceDetails = Lists.newArrayList(detail);
        when(jvppSnat.snatInterfaceDump(any())).thenReturn(future(details));

        assertTrue(getReader().read(id, ctx).isPresent());
    }

    @Test
    public void testPresencePostRouting() throws Exception {
        mockPostRoutingDump();
        assertTrue(getReader().read(id, ctx).isPresent());
    }

    @Test
    public void testReadPostRouting() throws Exception {
        mockPostRoutingDump();
        final InboundBuilder builder = mock(InboundBuilder.class);
        customizer.readCurrentAttributes(id, builder, ctx);
        verify(builder).setPostRouting(true);
    }

    @Override
    protected ReaderCustomizer<Inbound, InboundBuilder> initCustomizer() {
        return new InterfaceInboundNatCustomizer(jvppSnat, ifcContext);
    }
}