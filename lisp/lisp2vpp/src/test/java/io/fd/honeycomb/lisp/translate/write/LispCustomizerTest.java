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

import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispBuilder;
import org.openvpp.jvpp.core.dto.LispEnableDisable;
import org.openvpp.jvpp.core.dto.LispEnableDisableReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class LispCustomizerTest {

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        new LispCustomizer(mock(FutureJVppCore.class)).writeCurrentAttributes(null, null, null);
    }

    @Test
    public void testWriteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {
        FutureJVppCore fakeJvpp = mock(FutureJVppCore.class);
        Lisp intf = new LispBuilder().setEnable(true).build();

        ArgumentCaptor<LispEnableDisable> mappingCaptor = ArgumentCaptor.forClass(LispEnableDisable.class);

        LispEnableDisableReply fakeReply = new LispEnableDisableReply();

        CompletableFuture<LispEnableDisableReply> completeFuture = new CompletableFuture<>();
        completeFuture.complete(fakeReply);

        when(fakeJvpp.lispEnableDisable(any(LispEnableDisable.class))).thenReturn(completeFuture);

        new LispCustomizer(fakeJvpp).writeCurrentAttributes(null, intf, null);

        verify(fakeJvpp, times(1)).lispEnableDisable(mappingCaptor.capture());

        LispEnableDisable request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isEn);
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateCurrentAttributesNullData() throws WriteFailedException {
        new LispCustomizer(mock(FutureJVppCore.class)).updateCurrentAttributes(null, null, null, null);
    }

    @Test
    public void testUpdateCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        FutureJVppCore fakeJvpp = mock(FutureJVppCore.class);
        Lisp lisp = new LispBuilder().setEnable(true).build();

        ArgumentCaptor<LispEnableDisable> lispCaptor = ArgumentCaptor.forClass(LispEnableDisable.class);

        LispEnableDisableReply fakeReply = new LispEnableDisableReply();

        CompletableFuture<LispEnableDisableReply> completeFuture = new CompletableFuture<>();
        completeFuture.complete(fakeReply);

        when(fakeJvpp.lispEnableDisable(any(LispEnableDisable.class))).thenReturn(completeFuture);

        new LispCustomizer(fakeJvpp).updateCurrentAttributes(null, null, lisp, null);

        verify(fakeJvpp, times(1)).lispEnableDisable(lispCaptor.capture());

        LispEnableDisable request = lispCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isEn);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        new LispCustomizer(mock(FutureJVppCore.class)).deleteCurrentAttributes(null, null, null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        FutureJVppCore fakeJvpp = mock(FutureJVppCore.class);
        Lisp lisp = new LispBuilder().setEnable(true).build();

        ArgumentCaptor<LispEnableDisable> lispCaptor = ArgumentCaptor.forClass(LispEnableDisable.class);

        LispEnableDisableReply fakeReply = new LispEnableDisableReply();

        CompletableFuture<LispEnableDisableReply> completeFuture = new CompletableFuture<>();
        completeFuture.complete(fakeReply);

        when(fakeJvpp.lispEnableDisable(any(LispEnableDisable.class))).thenReturn(completeFuture);

        new LispCustomizer(fakeJvpp).deleteCurrentAttributes(null, lisp, null);

        verify(fakeJvpp, times(1)).lispEnableDisable(lispCaptor.capture());

        LispEnableDisable request = lispCaptor.getValue();

        assertNotNull(request);
        assertEquals(0, request.isEn);
    }

}
