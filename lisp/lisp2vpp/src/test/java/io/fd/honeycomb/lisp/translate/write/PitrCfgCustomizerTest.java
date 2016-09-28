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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.pitr.cfg.grouping.PitrCfgBuilder;
import io.fd.vpp.jvpp.core.dto.LispPitrSetLocatorSet;
import io.fd.vpp.jvpp.core.dto.LispPitrSetLocatorSetReply;


public class PitrCfgCustomizerTest extends WriterCustomizerTest {

    private PitrCfgCustomizer customizer;

    @Override
    public void setUp() {
        customizer = new PitrCfgCustomizer(api);
    }

    private void whenLispPitrSetLocatorSetThenSuccess() {
        when(api.lispPitrSetLocatorSet(any(LispPitrSetLocatorSet.class))).thenReturn(future(new LispPitrSetLocatorSetReply()));
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

        ArgumentCaptor<LispPitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(LispPitrSetLocatorSet.class);
        verify(api, times(1)).lispPitrSetLocatorSet(cfgCaptor.capture());

        LispPitrSetLocatorSet request = cfgCaptor.getValue();
        assertEquals(1, request.isAdd);
        assertEquals("Locator", new String(request.lsName));
    }

    @Test
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        PitrCfg cfg = new PitrCfgBuilder().setLocatorSet("Locator").build();

        whenLispPitrSetLocatorSetThenSuccess();

        customizer.writeCurrentAttributes(null, cfg, null);

        ArgumentCaptor<LispPitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(LispPitrSetLocatorSet.class);
        verify(api, times(1)).lispPitrSetLocatorSet(cfgCaptor.capture());

        LispPitrSetLocatorSet request = cfgCaptor.getValue();
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

        ArgumentCaptor<LispPitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(LispPitrSetLocatorSet.class);
        verify(api, times(1)).lispPitrSetLocatorSet(cfgCaptor.capture());

        LispPitrSetLocatorSet request = cfgCaptor.getValue();
        assertEquals(0, request.isAdd);
        assertEquals("Locator", new String(request.lsName));
    }

}
