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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeListReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeRootReader;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.read.DelegatingReaderRegistry;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.interfacesstate.InterfaceCustomizer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.openvpp.vppjapi.vppInterfaceDetails;
import org.openvpp.vppjapi.vppVersion;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("org.openvpp.vppjapi.vppConn")
@PrepareForTest(vppApi.class)
public class InterfaceCustomizerTest {

    public static final vppVersion VERSION = new vppVersion("test", "1", "2", "33");

    private vppApi api;
    private CompositeRootReader<InterfacesState, InterfacesStateBuilder> interfacesStateReader;
    private DelegatingReaderRegistry readerRegistry;
    private ReadContext ctx;

    private CompositeRootReader<InterfacesState, InterfacesStateBuilder> getInterfacesStateReader(
            final vppApi vppApi) {

        final CompositeListReader<Interface, InterfaceKey, InterfaceBuilder> interfacesReader =
                new CompositeListReader<>(Interface.class, new InterfaceCustomizer(vppApi));

        final List<ChildReader<? extends ChildOf<InterfacesState>>> childReaders = new ArrayList<>();
        childReaders.add(interfacesReader);

        return new CompositeRootReader<>(InterfacesState.class, childReaders,
                RWUtils.<InterfacesState>emptyAugReaderList(),
                new ReflexiveRootReaderCustomizer<>(InterfacesStateBuilder.class));
    }

    public static vppInterfaceDetails createVppInterfaceDetails(int ifIndex, String name) {
        return new vppInterfaceDetails(
                ifIndex, name, 0,
                new byte[]{ (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00},
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, 0, 0,
                (byte) 0, (byte) 0, (byte) 0, (byte) 0, 0, 0, 0, 0, 0);
    }

    @Before
    public void setUp() throws Exception {
        api = mock(vppApi.class);
        // PowerMockito.doReturn(VERSION).when(api).getVppVersion();
        ctx = mock(ReadContext.class);
        List<vppInterfaceDetails> ifaces = new ArrayList<>();
        ifaces.add(createVppInterfaceDetails(0, "loop0"));
        vppInterfaceDetails[] ifArr = ifaces.toArray(new vppInterfaceDetails[ifaces.size()]);

        PowerMockito.when(api.swInterfaceDump((byte) 0, new byte[]{})).
                thenReturn(ifArr);
        PowerMockito.when(api.swInterfaceDump((byte) 1, ifArr[0].interfaceName.getBytes())).thenReturn(ifArr);

        interfacesStateReader = getInterfacesStateReader(api);
        readerRegistry = new DelegatingReaderRegistry(
                Collections.<Reader<? extends DataObject>>singletonList(interfacesStateReader));
    }

    @Test
    public void testReadAll() throws ReadFailedException {
        final Multimap<InstanceIdentifier<? extends DataObject>, ? extends DataObject> dataObjects =
                readerRegistry.readAll(ctx);

        System.out.println(dataObjects.keys());
        final DataObject obj = Iterables.getOnlyElement(
                dataObjects.get(Iterables.getOnlyElement(dataObjects.keySet())));
        assertTrue(obj instanceof InterfacesState);
    }

    @Test
    public void testReadId() throws ReadFailedException {
        Optional<? extends DataObject> read =
                readerRegistry.read(InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey("Loofdsafdsap0")), ctx);
        System.err.println(read);
    }
}
