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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.InterfaceL3Module;
import io.fd.hc2vpp.l3.ProxyArpModule;
import io.fd.hc2vpp.l3.SubInterfaceL3Module;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.translate.impl.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.YangDAG;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class L3ModulesTest {

    @Bind
    @Mock
    private FutureJVppCore api;

    @Inject
    private Set<ReaderFactory> readerFactories = new HashSet<>();

    @Inject
    private Set<WriterFactory> writerFactories = new HashSet<>();

    @Bind
    @Named("interface-context")
    private NamingContext interfaceContext;

    @Before
    public void setUp() {
        initMocks(this);
        interfaceContext = new NamingContext("iface", "iface-context");
        // InterfaceL3Module adds readers under InterfacesState
        // because readers for parents need to be present (are part of V3poModule)
        // add structural readers here (better than maven dependency).
        Guice.createInjector(binder -> Multibinder.newSetBinder(binder, ReaderFactory.class)
                        .addBinding().toInstance(registry -> {
                            registry.addStructuralReader(InstanceIdentifier.create(InterfacesState.class),
                                    InterfacesStateBuilder.class);
                            registry.add(new GenericListReader<>(InstanceIdentifier.create(InterfacesState.class)
                                    .child(Interface.class),
                                    mock(ListReaderCustomizer.class)));
                        }), new InterfaceL3Module(),
                new SubInterfaceL3Module(),
                new ProxyArpModule(),
                BoundFieldModule.of(this)).injectMembers(this);
    }

    @Test
    public void testReaderFactories() throws Exception {
        assertThat(readerFactories, is(not(empty())));

        // Test registration process (all dependencies present, topological order of readers does exist, etc.)
        final CompositeReaderRegistryBuilder registryBuilder = new CompositeReaderRegistryBuilder(new YangDAG());
        readerFactories.stream().forEach(factory -> factory.init(registryBuilder));
        assertNotNull(registryBuilder.build());
    }

    @Test
    public void testWriterFactories() throws Exception {
        assertThat(writerFactories, is(not(empty())));

        // Test registration process (all dependencies present, topological order of writers does exist, etc.)
        final FlatWriterRegistryBuilder registryBuilder = new FlatWriterRegistryBuilder(new YangDAG());
        writerFactories.stream().forEach(factory -> factory.init(registryBuilder));
        assertNotNull(registryBuilder.build());
    }
}
