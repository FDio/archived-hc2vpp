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

package io.fd.honeycomb.nat.read.ifc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetails;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceDetailsReplyDump;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214.NatInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceInboundNatCustomizerTest
        extends ReaderCustomizerTest<Inbound, InboundBuilder> {

    private static final String IFC_NAME = "a";
    private static final int IFC_IDX = 0;
    private static final String CTX_NAME = "ifc";

    @Mock
    private EntityDumpExecutor<SnatInterfaceDetailsReplyDump, Void> natExecutor;
    private DumpCacheManager<SnatInterfaceDetailsReplyDump, Void> dumpMgr;
    private NamingContext ifcContext = new NamingContext(CTX_NAME, CTX_NAME);
    private InstanceIdentifier<Inbound> id;

    public InterfaceInboundNatCustomizerTest() {
        super(Inbound.class, NatBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        id = getId(Inbound.class);
        defineMapping(mappingContext, IFC_NAME, IFC_IDX, CTX_NAME);
        // empty dump
        Mockito.doReturn(new SnatInterfaceDetailsReplyDump()).when(natExecutor).executeDump(id, null);
        dumpMgr = new DumpCacheManager.DumpCacheManagerBuilder<SnatInterfaceDetailsReplyDump, Void>()
                .withExecutor(natExecutor)
                .build();
    }

    static <T extends ChildOf<Nat>> InstanceIdentifier<T> getId(Class<T> boundType) {
        return InstanceIdentifier.create(InterfacesState.class)
                .child(Interface.class, new InterfaceKey(IFC_NAME))
                .augmentation(NatInterfaceStateAugmentation.class)
                .child(Nat.class)
                .child(boundType);
    }

    @Test
    public void testNoPresence() throws Exception {
        assertFalse(getReader().read(id, ctx).isPresent());
    }

    private GenericReader<Inbound, InboundBuilder> getReader() {
        return new GenericReader<>(RWUtils.makeIidWildcarded(id), customizer);
    }

    @Test
    public void testPresence() throws Exception {
        final SnatInterfaceDetailsReplyDump details = new SnatInterfaceDetailsReplyDump();
        final SnatInterfaceDetails detail = new SnatInterfaceDetails();
        detail.isInside = 1;
        detail.swIfIndex = IFC_IDX;
        details.snatInterfaceDetails = Lists.newArrayList(detail);
        Mockito.doReturn(details).when(natExecutor).executeDump(id, null);

        assertTrue(getReader().read(id, ctx).isPresent());
    }

    @Override
    protected ReaderCustomizer<Inbound, InboundBuilder> initCustomizer() {
        return new InterfaceInboundNatCustomizer(dumpMgr, ifcContext);
    }
}