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

package io.fd.hc2vpp.management.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.impl.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.vpp.jvpp.core.dto.ShowVersion;
import io.fd.vpp.jvpp.core.dto.ShowVersionReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.dto.ControlPing;
import io.fd.vpp.jvpp.dto.ControlPingReply;
import javax.annotation.Nonnull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.management.rev170315.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.management.rev170315.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.management.rev170315.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.management.rev170315.vpp.state.VersionBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppStateTest implements FutureProducer {

    @Mock
    private FutureJVppCore api;
    @Mock
    private ReadContext ctx;
    @Mock
    private MappingContext mappingContext;

    private ReaderRegistry readerRegistry;

    /**
     * Create root VppState reader with all its children wired.
     */
    private static ReaderRegistry getVppStateReader(@Nonnull final FutureJVppCore jVpp) {
        final CompositeReaderRegistryBuilder registry = new CompositeReaderRegistryBuilder(new YangDAG());

        // VppState(Structural)
        final InstanceIdentifier<VppState> vppStateId = InstanceIdentifier.create(VppState.class);
        registry.addStructuralReader(vppStateId, VppStateBuilder.class);
        //  Version
        registry.add(new GenericReader<>(vppStateId.child(Version.class), new VersionCustomizer(jVpp)));
        return registry.build();
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        final ModificationCache cache = new ModificationCache();
        doReturn(cache).when(ctx).getModificationCache();
        doReturn(mappingContext).when(ctx).getMappingContext();


        readerRegistry = getVppStateReader(api);
    }

    private static Version getVersion() {
        return new VersionBuilder()
                .setName("test")
                .setBuildDirectory("1")
                .setBranch("2")
                .setBuildDate("3")
                .setPid(0L)
                .build();
    }

    private void whenShowVersionThenReturn(final Version version) {
        final ShowVersionReply reply = new ShowVersionReply();
        reply.buildDate = version.getBuildDate().getBytes();
        reply.program = version.getName().getBytes();
        reply.version = version.getBranch().getBytes();
        reply.buildDirectory = version.getBuildDirectory().getBytes();
        when(api.showVersion(ArgumentMatchers.any(ShowVersion.class))).thenReturn(future(reply));
        // Version Customizer uses ControlPing to obtain PID
        when(api.send(ArgumentMatchers.any(ControlPing.class))).thenReturn(future(new ControlPingReply()));
    }

    @Test
    public void testReadAll() throws Exception {
        final Version version = getVersion();
        whenShowVersionThenReturn(version);

        final Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> dataObjects =
                readerRegistry.readAll(ctx);
        assertEquals(dataObjects.size(), 1);
        final VppState dataObject =
                (VppState) Iterables.getOnlyElement(dataObjects.get(Iterables.getOnlyElement(dataObjects.keySet())));
        assertEquals(version, dataObject.getVersion());
    }

    @Test
    public void testReadSpecific() throws Exception {
        final Version version = getVersion();
        whenShowVersionThenReturn(version);

        final Optional<? extends DataObject> read = readerRegistry.read(InstanceIdentifier.create(VppState.class), ctx);
        assertTrue(read.isPresent());
        assertEquals(version, ((VppState) read.get()).getVersion());
    }

    @Test
    public void testReadVersion() throws Exception {
        whenShowVersionThenReturn(getVersion());
        VppState readRoot = (VppState) readerRegistry.read(InstanceIdentifier.create(VppState.class), ctx).get();

        Optional<? extends DataObject> read =
                readerRegistry.read(InstanceIdentifier.create(VppState.class).child(Version.class), ctx);
        assertTrue(read.isPresent());
        assertEquals(readRoot.getVersion(), read.get());
    }
}