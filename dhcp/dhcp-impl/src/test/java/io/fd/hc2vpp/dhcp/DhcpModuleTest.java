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

package io.fd.hc2vpp.dhcp;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import io.fd.hc2vpp.dhcp.read.DhcpReaderFactory;
import io.fd.hc2vpp.dhcp.write.DhcpWriterFactory;
import io.fd.honeycomb.translate.impl.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.translate.impl.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

public class DhcpModuleTest {

    @Named("honeycomb-context")
    @Bind
    @Mock
    private DataBroker honeycombContext;

    @Named("honeycomb-initializer")
    @Bind
    @Mock
    private DataBroker honeycombInitializer;

    @Bind
    @Mock
    private FutureJVppCore futureJVppCore;

    @Inject
    private Set<WriterFactory> writerFactories = new HashSet<>();

    @Inject
    private Set<ReaderFactory> readerFactories = new HashSet<>();

    @Before
    public void setUp() {
        initMocks(this);
        Guice.createInjector(new DhcpModule(), BoundFieldModule.of(this)).injectMembers(this);
    }

    @Test
    public void testWriterFactories() throws Exception {
        assertThat(writerFactories, is(not(empty())));

        // Test registration process (all dependencies present, topological order of writers does exist, etc.)
        final FlatWriterRegistryBuilder registryBuilder = new FlatWriterRegistryBuilder(new YangDAG());
        writerFactories.stream().forEach(factory -> factory.init(registryBuilder));
        assertNotNull(registryBuilder.build());
        assertEquals(1, writerFactories.size());
        assertTrue(writerFactories.iterator().next() instanceof DhcpWriterFactory);
    }

    @Test
    public void testReaderFactories() throws Exception {
        assertThat(readerFactories, is(not(empty())));

        // Test registration process (all dependencies present, topological order of readers does exist, etc.)
        final CompositeReaderRegistryBuilder registryBuilder = new CompositeReaderRegistryBuilder(new YangDAG());
        readerFactories.stream().forEach(factory -> factory.init(registryBuilder));
        assertNotNull(registryBuilder.build());
        assertEquals(1, readerFactories.size());
        assertTrue(readerFactories.iterator().next() instanceof DhcpReaderFactory);
    }
}
