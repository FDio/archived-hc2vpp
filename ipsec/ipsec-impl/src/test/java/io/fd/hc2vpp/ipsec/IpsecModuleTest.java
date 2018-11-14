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

package io.fd.hc2vpp.ipsec;

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
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import io.fd.hc2vpp.ipsec.read.IpsecReaderFactory;
import io.fd.hc2vpp.ipsec.write.IpsecWriterFactory;
import io.fd.honeycomb.translate.impl.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.translate.impl.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class IpsecModuleTest {

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
        Guice.createInjector(new IpsecModule(), BoundFieldModule.of(this)).injectMembers(this);
    }

    @Test
    public void testWriterFactories() throws Exception {
        assertThat(writerFactories, is(not(empty())));
        final FlatWriterRegistryBuilder registryBuilder = new FlatWriterRegistryBuilder(new YangDAG());
        writerFactories.stream().forEach(factory -> factory.init(registryBuilder));
        assertNotNull(registryBuilder.build());
        assertEquals(1, writerFactories.size());
        assertTrue(writerFactories.iterator().next() instanceof IpsecWriterFactory);
    }

    @Test
    public void testReaderFactories() throws Exception {
        assertThat(readerFactories, is(not(empty())));
        final CompositeReaderRegistryBuilder registryBuilder = new CompositeReaderRegistryBuilder(new YangDAG());
        readerFactories.stream().forEach(factory -> factory.init(registryBuilder));
        assertNotNull(registryBuilder.build());
        assertEquals(1, readerFactories.size());
        assertTrue(readerFactories.iterator().next() instanceof IpsecReaderFactory);
    }
}
