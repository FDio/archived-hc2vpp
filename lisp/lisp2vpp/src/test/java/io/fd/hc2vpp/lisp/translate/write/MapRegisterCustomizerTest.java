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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneMapRegisterEnableDisable;
import io.fd.vpp.jvpp.core.dto.OneMapRegisterEnableDisableReply;
import io.fd.vpp.jvpp.core.dto.OneMapRegisterFallbackThreshold;
import io.fd.vpp.jvpp.core.dto.OneMapRegisterFallbackThresholdReply;
import io.fd.vpp.jvpp.core.dto.OneMapRegisterSetTtl;
import io.fd.vpp.jvpp.core.dto.OneMapRegisterSetTtlReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.map.register.grouping.MapRegister;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.map.register.grouping.MapRegisterBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapRegisterCustomizerTest extends LispWriterCustomizerTest implements ByteDataTranslator {

    private static final InstanceIdentifier<MapRegister> ID = InstanceIdentifier.create(MapRegister.class);
    private MapRegisterCustomizer customizer;
    private MapRegister enabledRegister;
    private MapRegister disabledRegister;

    @Captor
    private ArgumentCaptor<OneMapRegisterEnableDisable> requestCaptor;

    @Captor
    private ArgumentCaptor<OneMapRegisterSetTtl> ttlRequestCaptor;

    @Captor
    private ArgumentCaptor<OneMapRegisterFallbackThreshold> fallbackRequestCaptor;

    private InstanceIdentifier<MapRegister> EMPTY_ID = InstanceIdentifier.create(MapRegister.class);
    private MapRegister EMPTY_DATA = new MapRegisterBuilder().setEnabled(false).build();


    @Override
    protected void setUpTest() throws Exception {
        customizer = new MapRegisterCustomizer(api, lispStateCheckService);

        enabledRegister = new MapRegisterBuilder()
                .setEnabled(true)
                .setTtl(7L)
                .setFallbackThreshold(4L)
                .build();

        disabledRegister = new MapRegisterBuilder()
                .setEnabled(false)
                .build();

        when(api.oneMapRegisterEnableDisable(any(OneMapRegisterEnableDisable.class)))
                .thenReturn(future(new OneMapRegisterEnableDisableReply()));
        when(api.oneMapRegisterSetTtl(any(OneMapRegisterSetTtl.class)))
                .thenReturn(future(new OneMapRegisterSetTtlReply()));
        when(api.oneMapRegisterFallbackThreshold(any(OneMapRegisterFallbackThreshold.class)))
                .thenReturn(future(new OneMapRegisterFallbackThresholdReply()));
    }

    @Test
    public void writeCurrentAttributes() throws Exception {
        customizer.writeCurrentAttributes(ID, enabledRegister, writeContext);
        verifyRequest(true, 7, 4);
    }

    @Test
    public void updateCurrentAttributesToDisabled() throws Exception {
        customizer.updateCurrentAttributes(ID, enabledRegister, disabledRegister, writeContext);
        verifyRequest(false, 7, 4);
    }

    @Test
    public void updateCurrentAttributesToEnabled() throws Exception {
        customizer.updateCurrentAttributes(ID, disabledRegister, enabledRegister, writeContext);
        verifyRequest(true, 7, 4);
    }

    @Test
    public void deleteCurrentAttributes() throws Exception {
        customizer.deleteCurrentAttributes(ID, disabledRegister, writeContext);
        verifyRequest(false, 0, 0);
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
            customizer.updateCurrentAttributes(EMPTY_ID, EMPTY_DATA, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    @Test
    public void testDeleteLispDisabled() throws WriteFailedException {
        mockLispDisabledBefore();
        try {
            customizer.deleteCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    private void verifyRequest(final boolean enabled, final int ttl,final int fallback) {
        verify(api, times(1)).oneMapRegisterEnableDisable(requestCaptor.capture());

        final OneMapRegisterEnableDisable request = requestCaptor.getValue();
        assertEquals(booleanToByte(enabled), request.isEnabled);

        if (enabled) {
            verify(api, times(1)).oneMapRegisterSetTtl(ttlRequestCaptor.capture());
            final OneMapRegisterSetTtl ttlRequest = ttlRequestCaptor.getValue();
            assertEquals(ttl, ttlRequest.ttl);

            verify(api, times(1)).oneMapRegisterFallbackThreshold(fallbackRequestCaptor.capture());
            final OneMapRegisterFallbackThreshold fallbackRequest = fallbackRequestCaptor.getValue();
            assertEquals(fallback, fallbackRequest.value);
        } else {
            verifyNoMoreInteractions(api);
        }
    }
}
