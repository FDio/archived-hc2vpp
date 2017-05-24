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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.GpeEnableDisable;
import io.fd.vpp.jvpp.core.dto.GpeEnableDisableReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.feature.data.grouping.GpeFeatureDataBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GpeFeatureCustomizerTest extends WriterCustomizerTest {

    private GpeFeatureCustomizer customizer;

    @Captor
    private ArgumentCaptor<GpeEnableDisable> requestCaptor;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new GpeFeatureCustomizer(api);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        when(api.gpeEnableDisable(any())).thenReturn(future(new GpeEnableDisableReply()));

        customizer.writeCurrentAttributes(InstanceIdentifier.create(GpeFeatureData.class),
                new GpeFeatureDataBuilder().setEnable(true).build(), writeContext);
        verify(api, times(1)).gpeEnableDisable(requestCaptor.capture());
        final GpeEnableDisable request = requestCaptor.getValue();
        assertEquals(1, request.isEn);
    }

    @Test
    public void testDelete() throws WriteFailedException {
        when(api.gpeEnableDisable(any())).thenReturn(future(new GpeEnableDisableReply()));

        customizer.deleteCurrentAttributes(InstanceIdentifier.create(GpeFeatureData.class),
                new GpeFeatureDataBuilder().setEnable(true).build(), writeContext);
        verify(api, times(1)).gpeEnableDisable(requestCaptor.capture());
        final GpeEnableDisable request = requestCaptor.getValue();
        assertEquals(0, request.isEn);
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        when(api.gpeEnableDisable(any())).thenReturn(future(new GpeEnableDisableReply()));

        customizer.writeCurrentAttributes(InstanceIdentifier.create(GpeFeatureData.class),
                new GpeFeatureDataBuilder().setEnable(false).build(), writeContext);
        verify(api, times(1)).gpeEnableDisable(requestCaptor.capture());
        final GpeEnableDisable request = requestCaptor.getValue();
        assertEquals(0, request.isEn);
    }
}
