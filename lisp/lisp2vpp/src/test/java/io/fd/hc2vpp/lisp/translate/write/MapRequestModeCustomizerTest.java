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

package io.fd.hc2vpp.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.MapRequestMode.DestinationOnly;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.MapRequestMode.SourceDestination;

import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneMapRequestMode;
import io.fd.vpp.jvpp.core.dto.OneMapRequestModeReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.map.request.mode.grouping.MapRequestMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.map.request.mode.grouping.MapRequestModeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRequestModeCustomizerTest extends LispWriterCustomizerTest {

    private static final InstanceIdentifier<MapRequestMode> ID = InstanceIdentifier.create(MapRequestMode.class);
    private MapRequestModeCustomizer customizer;
    private MapRequestMode sourceDestinationMode;
    private MapRequestMode destinationOnlyMode;

    @Captor
    private ArgumentCaptor<OneMapRequestMode> requestCaptor;
    private InstanceIdentifier<MapRequestMode> EMPTY_ID = InstanceIdentifier.create(MapRequestMode.class);
    private MapRequestMode EMPTY_DATA = new MapRequestModeBuilder().build();

    @Override
    protected void setUpTest() throws Exception {
        customizer = new MapRequestModeCustomizer(api, lispStateCheckService);
        sourceDestinationMode = new MapRequestModeBuilder()
                .setMode(SourceDestination)
                .build();
        destinationOnlyMode = new MapRequestModeBuilder()
                .setMode(DestinationOnly)
                .build();
        when(api.oneMapRequestMode(any(OneMapRequestMode.class))).thenReturn(future(new OneMapRequestModeReply()));
    }

    @Test
    public void writeCurrentAttributes() throws Exception {
        customizer.writeCurrentAttributes(ID, sourceDestinationMode, writeContext);
        verifyModeRequest(SourceDestination);
    }

    @Test
    public void updateCurrentAttributes() throws Exception {
        customizer.updateCurrentAttributes(ID, sourceDestinationMode, destinationOnlyMode, writeContext);
        verifyModeRequest(DestinationOnly);
    }

    @Test
    public void deleteCurrentAttributes() throws Exception {
        verify(api, times(0)).lispMapRequestMode(any());
    }

    @Test
    public void testWriteLispDisabled() throws WriteFailedException {
        mockLispDisabledAfter();
        try {
            customizer.writeCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    @Test
    public void testUpdateLispDisabled() throws WriteFailedException {
        mockLispDisabledAfter();
        try {
            customizer.updateCurrentAttributes(EMPTY_ID, EMPTY_DATA,EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    private void verifyModeRequest(
            final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.MapRequestMode mode) {
        verify(api, times(1)).oneMapRequestMode(requestCaptor.capture());

        final OneMapRequestMode request = requestCaptor.getValue();
        assertEquals(mode.getIntValue(), request.mode);
    }
}