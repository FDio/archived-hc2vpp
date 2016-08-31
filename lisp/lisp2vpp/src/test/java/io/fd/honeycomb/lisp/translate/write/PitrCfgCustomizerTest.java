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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.pitr.cfg.grouping.PitrCfgBuilder;
import org.openvpp.jvpp.core.dto.LispPitrSetLocatorSet;
import org.openvpp.jvpp.core.dto.LispPitrSetLocatorSetReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class PitrCfgCustomizerTest {

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        new PitrCfgCustomizer(mock(FutureJVppCore.class)).writeCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesBadData() throws WriteFailedException {
        new PitrCfgCustomizer(mock(FutureJVppCore.class)).writeCurrentAttributes(null, mock(PitrCfg.class), null);
    }

    @Test
    public void testWriteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {
        FutureJVppCore fakeJvpp = mock(FutureJVppCore.class);

        PitrCfgCustomizer customizer = new PitrCfgCustomizer(fakeJvpp);
        PitrCfg cfg = new PitrCfgBuilder().setLocatorSet("Locator").build();

        ArgumentCaptor<LispPitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(LispPitrSetLocatorSet.class);

        LispPitrSetLocatorSetReply fakeReply = new LispPitrSetLocatorSetReply();

        CompletableFuture<LispPitrSetLocatorSetReply> finalStage = new CompletableFuture<>();
        finalStage.complete(fakeReply);

        when(fakeJvpp.lispPitrSetLocatorSet(any(LispPitrSetLocatorSet.class))).thenReturn(finalStage);

        customizer.writeCurrentAttributes(null, cfg, null);
        verify(fakeJvpp, times(1)).lispPitrSetLocatorSet(cfgCaptor.capture());

        LispPitrSetLocatorSet request = cfgCaptor.getValue();
        assertEquals(1, request.isAdd);
        assertEquals("Locator", new String(request.lsName));
    }

    @Test
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        FutureJVppCore fakeJvpp = mock(FutureJVppCore.class);

        PitrCfgCustomizer customizer = new PitrCfgCustomizer(fakeJvpp);
        PitrCfg cfg = new PitrCfgBuilder().setLocatorSet("Locator").build();

        ArgumentCaptor<LispPitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(LispPitrSetLocatorSet.class);

        LispPitrSetLocatorSetReply fakeReply = new LispPitrSetLocatorSetReply();

        CompletableFuture<LispPitrSetLocatorSetReply> finalStage = new CompletableFuture<>();
        finalStage.complete(fakeReply);

        when(fakeJvpp.lispPitrSetLocatorSet(any(LispPitrSetLocatorSet.class))).thenReturn(finalStage);

        customizer.writeCurrentAttributes(null, cfg, null);
        verify(fakeJvpp, times(1)).lispPitrSetLocatorSet(cfgCaptor.capture());

        LispPitrSetLocatorSet request = cfgCaptor.getValue();
        assertEquals(1, request.isAdd);
        assertEquals("Locator", new String(request.lsName));
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        new PitrCfgCustomizer(mock(FutureJVppCore.class)).deleteCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesBadData() throws WriteFailedException {
        new PitrCfgCustomizer(mock(FutureJVppCore.class)).deleteCurrentAttributes(null, mock(PitrCfg.class), null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        FutureJVppCore fakeJvpp = mock(FutureJVppCore.class);

        PitrCfgCustomizer customizer = new PitrCfgCustomizer(fakeJvpp);
        PitrCfg cfg = new PitrCfgBuilder().setLocatorSet("Locator").build();

        ArgumentCaptor<LispPitrSetLocatorSet> cfgCaptor = ArgumentCaptor.forClass(LispPitrSetLocatorSet.class);

        LispPitrSetLocatorSetReply fakeReply = new LispPitrSetLocatorSetReply();

        CompletableFuture<LispPitrSetLocatorSetReply> finalStage = new CompletableFuture<>();
        finalStage.complete(fakeReply);

        when(fakeJvpp.lispPitrSetLocatorSet(any(LispPitrSetLocatorSet.class))).thenReturn(finalStage);

        customizer.deleteCurrentAttributes(null, cfg, null);
        verify(fakeJvpp, times(1)).lispPitrSetLocatorSet(cfgCaptor.capture());

        LispPitrSetLocatorSet request = cfgCaptor.getValue();
        assertEquals(0, request.isAdd);
        assertEquals("Locator", new String(request.lsName));
    }

}
