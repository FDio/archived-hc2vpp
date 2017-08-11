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

package io.fd.hc2vpp.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OnePitrSetLocatorSet;
import io.fd.vpp.jvpp.core.dto.OnePitrSetLocatorSetReply;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.pitr.cfg.grouping.PitrCfgBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class PitrCfgCustomizerTest extends LispWriterCustomizerTest {

    private PitrCfgCustomizer customizer;
    private InstanceIdentifier<PitrCfg> EMPTY_ID = InstanceIdentifier.create(PitrCfg.class);
    private PitrCfg EMPTY_DATA = new PitrCfgBuilder().build();

    @Override
    public void setUpTest() {
        customizer = new PitrCfgCustomizer(api, lispStateCheckService);
    }

    private void whenLispPitrSetLocatorSetThenSuccess() {
        when(api.onePitrSetLocatorSet(any(OnePitrSetLocatorSet.class))).thenReturn(future(new OnePitrSetLocatorSetReply()));
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesBadData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, mock(PitrCfg.class), null);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {
        PitrCfg cfg = new PitrCfgBuilder().setLocatorSet("Locator").build();

        whenLispPitrSetLocatorSetThenSuccess();
        customizer.writeCurrentAttributes(null, cfg, null);

        ArgumentCaptor<OnePitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(OnePitrSetLocatorSet.class);
        verify(api, times(1)).onePitrSetLocatorSet(cfgCaptor.capture());

        OnePitrSetLocatorSet request = cfgCaptor.getValue();
        assertEquals(1, request.isAdd);
        assertEquals("Locator", new String(request.lsName));
    }

    @Test
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        PitrCfg cfg = new PitrCfgBuilder().setLocatorSet("Locator").build();

        whenLispPitrSetLocatorSetThenSuccess();

        customizer.writeCurrentAttributes(null, cfg, null);

        ArgumentCaptor<OnePitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(OnePitrSetLocatorSet.class);
        verify(api, times(1)).onePitrSetLocatorSet(cfgCaptor.capture());

        OnePitrSetLocatorSet request = cfgCaptor.getValue();
        assertEquals(1, request.isAdd);
        assertEquals("Locator", new String(request.lsName));
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        customizer.deleteCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesBadData() throws WriteFailedException {
        customizer.deleteCurrentAttributes(null, mock(PitrCfg.class), null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        PitrCfg cfg = new PitrCfgBuilder().setLocatorSet("Locator").build();

        whenLispPitrSetLocatorSetThenSuccess();

        customizer.deleteCurrentAttributes(null, cfg, null);

        ArgumentCaptor<OnePitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(OnePitrSetLocatorSet.class);
        verify(api, times(1)).onePitrSetLocatorSet(cfgCaptor.capture());

        OnePitrSetLocatorSet request = cfgCaptor.getValue();
        assertEquals(0, request.isAdd);
        assertEquals("Locator", new String(request.lsName));
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

}
