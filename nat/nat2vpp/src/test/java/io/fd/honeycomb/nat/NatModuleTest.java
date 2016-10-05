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

package io.fd.honeycomb.nat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.util.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.translate.util.write.registry.FlatWriterRegistryBuilder;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NatModuleTest {

    @Named("honeycomb-context")
    @Bind
    @Mock
    private DataBroker honeycombContext;

    @Bind
    private ReaderFactory ietfIfcReaderFactory;

    @Named("honeycomb-initializer")
    @Bind
    @Mock
    private DataBroker honeycombInitializer;

    @Named("interface-context")
    @Bind
    private NamingContext ifcContext;

    @Inject
    private Set<ReaderFactory> readerFactories = new HashSet<>();

    @Inject
    private Set<WriterFactory> writerFactories = new HashSet<>();

    @Before
    public void setUp() throws Exception {
        ietfIfcReaderFactory = registry -> {
            registry.addStructuralReader(InstanceIdentifier.create(InterfacesState.class), InterfacesStateBuilder.class);
            registry.addStructuralReader(InstanceIdentifier.create(InterfacesState.class).child(Interface.class), InterfaceBuilder.class);
        };
        initMocks(this);
        ifcContext = new NamingContext("interface-", "interface-context");
        // Nat Module adds readers under InterfacesState/Interface and since readers for parents that do nothing need to
        // be present, add structural readers (or add V3poModule here, but adding the full Module is not the best solution)
        Guice.createInjector(binder -> Multibinder.newSetBinder(binder, ReaderFactory.class)
                .addBinding().toInstance(registry -> {
                    registry.addStructuralReader(InstanceIdentifier.create(InterfacesState.class),
                            InterfacesStateBuilder.class);
                    registry.addStructuralReader(InstanceIdentifier.create(InterfacesState.class).child(Interface.class),
                            InterfaceBuilder.class);
                }), new NatModule(MockJVppSnatProvider.class), BoundFieldModule.of(this)).injectMembers(this);
    }

    @Test
    public void testReaderFactories() throws Exception {
        assertThat(readerFactories, is(not(empty())));

        // Test registration process (all dependencies present, topological order of readers does exist, etc.)
        final CompositeReaderRegistryBuilder registryBuilder = new CompositeReaderRegistryBuilder();
        readerFactories.forEach(factory -> factory.init(registryBuilder));
        assertNotNull(registryBuilder.build());
    }

    @Test
    public void testWriterFactories() throws Exception {
        assertThat(writerFactories, is(not(empty())));

        // Test registration process (all dependencies present, topological order of writers does exist, etc.)
        final FlatWriterRegistryBuilder registryBuilder = new FlatWriterRegistryBuilder();
        writerFactories.forEach(factory -> factory.init(registryBuilder));
        assertNotNull(registryBuilder.build());
    }

    private static final class MockJVppSnatProvider implements Provider<FutureJVppSnatFacade> {

        @Override
        public FutureJVppSnatFacade get() {
            return mock(FutureJVppSnatFacade.class);
        }
    }
}