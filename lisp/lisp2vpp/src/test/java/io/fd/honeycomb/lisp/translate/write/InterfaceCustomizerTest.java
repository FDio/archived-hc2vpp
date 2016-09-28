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

import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispAddDelLocator;
import io.fd.vpp.jvpp.core.dto.LispAddDelLocatorReply;

public class InterfaceCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    @Captor
    private ArgumentCaptor<LispAddDelLocator> intfCaptor;

    private InstanceIdentifier<Interface> id;
    private Interface intf;
    private InterfaceCustomizer customizer;

    @Override
    public void setUp() {
        final String ifcCtxName = "INInterruptedException, ExecutionException, STANCE";
        final String interfaceName = "Interface";
        defineMapping(mappingContext, interfaceName, 5, ifcCtxName);

        id = InstanceIdentifier.builder(Lisp.class)
                .child(LocatorSets.class)
                .child(LocatorSet.class, new LocatorSetKey("Locator"))
            .child(Interface.class, new InterfaceKey(interfaceName))
                .build();

        intf = new InterfaceBuilder()
                .setPriority((short) 1)
                .setWeight((short) 2)
                .build();

        customizer = new InterfaceCustomizer(api, new NamingContext("PREFIX", ifcCtxName));

        when(api.lispAddDelLocator(any(LispAddDelLocator.class))).thenReturn(future(new LispAddDelLocatorReply()));
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullPriority() throws WriteFailedException {
        Interface intf = mock(Interface.class);
        when(intf.getWeight()).thenReturn((short) 1);
        when(intf.getPriority()).thenReturn(null);

        customizer.writeCurrentAttributes(null, intf, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullWeight() throws WriteFailedException {
        Interface intf = mock(Interface.class);
        when(intf.getWeight()).thenReturn(null);
        when(intf.getPriority()).thenReturn((short) 1);

        customizer.writeCurrentAttributes(null, intf, null);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {
        customizer.writeCurrentAttributes(id, intf, writeContext);

        verify(api, times(1)).lispAddDelLocator(intfCaptor.capture());

        LispAddDelLocator request = intfCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isAdd);
        assertEquals(2, request.weight);
        assertEquals(1, request.priority);
        assertEquals(5, request.swIfIndex);
        assertEquals("Locator", toString(request.locatorSetName));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        customizer.updateCurrentAttributes(null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        customizer.deleteCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullPriority() throws WriteFailedException {
        Interface interf = mock(Interface.class);
        when(interf.getWeight()).thenReturn((short) 1);
        when(interf.getPriority()).thenReturn(null);

        customizer.deleteCurrentAttributes(null, interf, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullWeight() throws WriteFailedException {
        Interface interf = mock(Interface.class);
        when(interf.getWeight()).thenReturn(null);
        when(interf.getPriority()).thenReturn((short) 1);

        customizer.deleteCurrentAttributes(null, interf, null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException {
        customizer.deleteCurrentAttributes(id, intf, writeContext);

        verify(api, times(1)).lispAddDelLocator(intfCaptor.capture());

        LispAddDelLocator request = intfCaptor.getValue();

        assertNotNull(request);
        assertEquals(0, request.isAdd);
        assertEquals(2, request.weight);
        assertEquals(1, request.priority);
        assertEquals(5, request.swIfIndex);
        assertEquals("Locator", toString(request.locatorSetName));
    }
}
