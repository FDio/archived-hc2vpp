/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispAddDelLocator;
import org.openvpp.jvpp.core.dto.LispAddDelLocatorReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;

public class InterfaceCustomizerTest extends WriterCustomizerTest {

    @Captor
    private ArgumentCaptor<LispAddDelLocator> intfCaptor;

    private NamingContext namingContext;
    private InstanceIdentifier<Interface> id;
    private Interface intf;
    private InterfaceCustomizer customizer;

    @Override
    public void setUp() {
        id = InstanceIdentifier.builder(Lisp.class)
                .child(LocatorSets.class)
                .child(LocatorSet.class, new LocatorSetKey("Locator"))
                .child(Interface.class, new InterfaceKey("Interface"))
                .build();

        intf = new InterfaceBuilder()
                .setPriority((short) 1)
                .setWeight((short) 2)
                .build();

        namingContext = new NamingContext("PREFIX", "INSTANCE");

        customizer = new InterfaceCustomizer(api, namingContext);

        when(mappingContext.read(Mockito.any()))
                .thenReturn(Optional.of((DataObject) new MappingBuilder().setIndex(5).setName("interface").build()));
        when(api.lispAddDelLocator(any(LispAddDelLocator.class))).thenReturn(future(new LispAddDelLocatorReply()));
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        new InterfaceCustomizer(mock(FutureJVppCore.class), new NamingContext("PREFIX", "INSTANCE"))
                .writeCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullPriority() throws WriteFailedException {
        Interface intf = mock(Interface.class);
        when(intf.getWeight()).thenReturn((short) 1);
        when(intf.getPriority()).thenReturn(null);

        new InterfaceCustomizer(mock(FutureJVppCore.class), new NamingContext("PREFIX", "INSTANCE"))
                .writeCurrentAttributes(null, intf, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullWeight() throws WriteFailedException {
        Interface intf = mock(Interface.class);
        when(intf.getWeight()).thenReturn(null);
        when(intf.getPriority()).thenReturn((short) 1);

        new InterfaceCustomizer(mock(FutureJVppCore.class), new NamingContext("PREFIX", "INSTANCE"))
                .writeCurrentAttributes(null, intf, null);
    }

    @Test
    public void testWriteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {
        customizer.writeCurrentAttributes(id, intf, writeContext);

        verify(api, times(1)).lispAddDelLocator(intfCaptor.capture());

        LispAddDelLocator request = intfCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isAdd);
        assertEquals(2, request.weight);
        assertEquals(1, request.priority);
        assertEquals(5, request.swIfIndex);
        assertEquals("Locator", TranslateUtils.toString(request.locatorSetName));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        new InterfaceCustomizer(api, namingContext)
                .updateCurrentAttributes(null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        new InterfaceCustomizer(api, namingContext)
                .deleteCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullPriority() throws WriteFailedException {
        Interface interf = mock(Interface.class);
        when(interf.getWeight()).thenReturn((short) 1);
        when(interf.getPriority()).thenReturn(null);

        new InterfaceCustomizer(mock(FutureJVppCore.class), new NamingContext("PREFIX", "INSTANCE"))
                .deleteCurrentAttributes(null, interf, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullWeight() throws WriteFailedException {
        Interface interf = mock(Interface.class);
        when(interf.getWeight()).thenReturn(null);
        when(interf.getPriority()).thenReturn((short) 1);

        new InterfaceCustomizer(mock(FutureJVppCore.class), new NamingContext("PREFIX", "INSTANCE"))
                .deleteCurrentAttributes(null, interf, null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {
        customizer.deleteCurrentAttributes(id, intf, writeContext);

        verify(api, times(1)).lispAddDelLocator(intfCaptor.capture());

        LispAddDelLocator request = intfCaptor.getValue();

        assertNotNull(request);
        assertEquals(0, request.isAdd);
        assertEquals(2, request.weight);
        assertEquals(1, request.priority);
        assertEquals(5, request.swIfIndex);
        assertEquals("Locator", TranslateUtils.toString(request.locatorSetName));
    }
}
