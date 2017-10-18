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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneEnableDisable;
import io.fd.vpp.jvpp.core.dto.OneEnableDisableReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.LispBuilder;

public class LispCustomizerTest extends WriterCustomizerTest {

    private LispCustomizer customizer;

    @Override
    public void setUpTest() {
        customizer = new LispCustomizer(api);
    }

    private void whenlispEnableDisableThenSuccess() {
        when(api.oneEnableDisable(any(OneEnableDisable.class))).thenReturn(future(new OneEnableDisableReply()));
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, null, null);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException {
        Lisp intf = new LispBuilder().setEnable(true).build();

        whenlispEnableDisableThenSuccess();
        customizer.writeCurrentAttributes(null, intf, null);

        ArgumentCaptor<OneEnableDisable> mappingCaptor = ArgumentCaptor.forClass(OneEnableDisable.class);
        verify(api, times(1)).oneEnableDisable(mappingCaptor.capture());

        OneEnableDisable request = mappingCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isEn);
    }

    @Test(expected = NullPointerException.class)
    public void testUpdateCurrentAttributesNullData() throws WriteFailedException {
        customizer.updateCurrentAttributes(null, null, null, null);
    }

    @Test
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        Lisp lisp = new LispBuilder().setEnable(true).build();

        whenlispEnableDisableThenSuccess();
        customizer.updateCurrentAttributes(null, null, lisp, null);

        ArgumentCaptor<OneEnableDisable> lispCaptor = ArgumentCaptor.forClass(OneEnableDisable.class);
        verify(api, times(1)).oneEnableDisable(lispCaptor.capture());

        OneEnableDisable request = lispCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isEn);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        customizer.deleteCurrentAttributes(null, null, null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws WriteFailedException {
        Lisp lisp = new LispBuilder().setEnable(true).build();

        whenlispEnableDisableThenSuccess();
        customizer.deleteCurrentAttributes(null, lisp, null);

        ArgumentCaptor<OneEnableDisable> lispCaptor = ArgumentCaptor.forClass(OneEnableDisable.class);
        verify(api, times(1)).oneEnableDisable(lispCaptor.capture());

        OneEnableDisable request = lispCaptor.getValue();

        assertNotNull(request);
        assertEquals(0, request.isEn);
    }
}
