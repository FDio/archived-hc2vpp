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

package io.fd.hc2vpp.lisp.gpe.translate.read;


import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.jvpp.core.dto.ShowLispStatusReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.Gpe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.GpeState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.GpeStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureDataBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class GpeFeatureCustomizerTest extends InitializingReaderCustomizerTest<GpeFeatureData, GpeFeatureDataBuilder> {
    public GpeFeatureCustomizerTest() {
        super(GpeFeatureData.class, GpeStateBuilder.class);
    }

    @Override
    protected GpeFeatureCustomizer initCustomizer() {
        return new GpeFeatureCustomizer(api);
    }

    @Test
    public void testReadCurrent() throws ReadFailedException {
        final ShowLispStatusReply result = new ShowLispStatusReply();
        result.gpeStatus = 1;
        when(api.showLispStatus(any())).thenReturn(future(result));

        final GpeFeatureDataBuilder builder = new GpeFeatureDataBuilder();
        getCustomizer().readCurrentAttributes(InstanceIdentifier.create(GpeFeatureData.class), builder, ctx);
        assertTrue(builder.isEnable());
    }

    @Test
    public void testInit() {
        final InstanceIdentifier<GpeFeatureData> CONFIG_ID =
                InstanceIdentifier.create(Gpe.class).child(GpeFeatureData.class);

        final InstanceIdentifier<GpeFeatureData> STATE_ID =
                InstanceIdentifier.create(GpeState.class).child(GpeFeatureData.class);

        final GpeFeatureData data = new GpeFeatureDataBuilder().build();
        invokeInitTest(STATE_ID, data, CONFIG_ID, data);
    }
}
