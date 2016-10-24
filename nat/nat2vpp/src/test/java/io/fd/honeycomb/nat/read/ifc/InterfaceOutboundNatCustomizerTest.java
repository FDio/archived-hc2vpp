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

import static io.fd.honeycomb.nat.read.ifc.InterfaceInboundNatCustomizerTest.getId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.NatBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.Outbound;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.OutboundBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceOutboundNatCustomizerTest
        extends ReaderCustomizerTest<Outbound, OutboundBuilder> {

    private static final String IFC_NAME = "a";
    private static final int IFC_IDX = 0;
    private static final String CTX_NAME = "ifc";

    @Mock
    private EntityDumpExecutor<SnatInterfaceDetailsReplyDump, Void> abc;
    private DumpCacheManager<SnatInterfaceDetailsReplyDump, Void> dumpMgr;
    private NamingContext ifcContext = new NamingContext(CTX_NAME, CTX_NAME);
    private InstanceIdentifier<Outbound> id;

    public InterfaceOutboundNatCustomizerTest() {
        super(Outbound.class, NatBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        id = getId(Outbound.class);
        defineMapping(mappingContext, IFC_NAME, IFC_IDX, CTX_NAME);
        // empty dump
        Mockito.doReturn(new SnatInterfaceDetailsReplyDump()).when(abc).executeDump(id, null);
        dumpMgr = new DumpCacheManager.DumpCacheManagerBuilder<SnatInterfaceDetailsReplyDump, Void>()
                .withExecutor(abc)
                .build();
    }

    @Test
    public void testNoPresence() throws Exception {
        assertFalse(getReader().read(id, ctx).isPresent());
    }

    private GenericReader<Outbound, OutboundBuilder> getReader() {
        return new GenericReader<>(RWUtils.makeIidWildcarded(id), customizer);
    }

    @Test
    public void testPresence() throws Exception {
        final SnatInterfaceDetailsReplyDump details = new SnatInterfaceDetailsReplyDump();
        final SnatInterfaceDetails detail = new SnatInterfaceDetails();
        detail.isInside = 0;
        detail.swIfIndex = IFC_IDX;
        details.snatInterfaceDetails = Lists.newArrayList(detail);
        Mockito.doReturn(details).when(abc).executeDump(id, null);

        assertTrue(getReader().read(id, ctx).isPresent());
    }

    @Override
    protected ReaderCustomizer<Outbound, OutboundBuilder> initCustomizer() {
        return new InterfaceOutboundNatCustomizer(dumpMgr, ifcContext);
    }
}