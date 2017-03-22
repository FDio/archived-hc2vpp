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

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.LispRlocProbeEnableDisable;
import io.fd.vpp.jvpp.core.dto.LispRlocProbeEnableDisableReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.rloc.probing.grouping.RlocProbe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.rloc.probing.grouping.RlocProbeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RlocProbeCustomizerTest extends LispWriterCustomizerTest implements ByteDataTranslator {

    private static final InstanceIdentifier<RlocProbe> ID = InstanceIdentifier.create(RlocProbe.class);
    private RlocProbeCustomizer customizer;
    private RlocProbe enabledProbe;
    private RlocProbe disabledProbe;

    @Captor
    private ArgumentCaptor<LispRlocProbeEnableDisable> requestCaptor;
    private InstanceIdentifier<RlocProbe> EMPTY_ID = InstanceIdentifier.create(RlocProbe.class);
    private RlocProbe EMPTY_DATA = new RlocProbeBuilder().build();

    @Override
    protected void setUpTest() throws Exception {
        customizer = new RlocProbeCustomizer(api, lispStateCheckService);
        enabledProbe = rlocProbe(true);
        disabledProbe = rlocProbe(false);
        when(api.lispRlocProbeEnableDisable(any(LispRlocProbeEnableDisable.class)))
                .thenReturn(future(new LispRlocProbeEnableDisableReply()));
    }

    @Test
    public void testWriteCurrentAttributes() throws Exception {
        customizer.writeCurrentAttributes(ID, enabledProbe, writeContext);
        verifyRequest(true);
    }

    @Test
    public void testUpdateCurrentAttributesToDisabled() throws Exception {
        customizer.updateCurrentAttributes(ID, enabledProbe, disabledProbe, writeContext);
        verifyRequest(false);
    }

    @Test
    public void testUpdateCurrentAttributesToEnabled() throws Exception {
        customizer.updateCurrentAttributes(ID, disabledProbe, enabledProbe, writeContext);
        verifyRequest(true);
    }

    @Test
    public void testDeleteCurrentAttributes() throws Exception {
        customizer.deleteCurrentAttributes(ID, disabledProbe, writeContext);
        verifyRequest(false);
    }

    @Test
    public void testWriteLispDisabled() throws WriteFailedException {
        mockLispDisabled();
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
        mockLispDisabled();
        try {
            customizer.updateCurrentAttributes(EMPTY_ID, EMPTY_DATA,EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    @Test
    public void testDeleteLispDisabled() throws WriteFailedException {
        mockLispDisabled();
        try {
            customizer.deleteCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    private static RlocProbe rlocProbe(final boolean enabled) {
        return new RlocProbeBuilder().setEnabled(enabled).build();
    }

    private void verifyRequest(final boolean enabled) {
        verify(api, times(1)).lispRlocProbeEnableDisable(requestCaptor.capture());
        final LispRlocProbeEnableDisable request = requestCaptor.getValue();
        assertEquals(booleanToByte(enabled), request.isEnabled);
    }
}