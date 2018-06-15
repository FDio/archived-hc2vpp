/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.write.encap.source;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.srv6.Srv6IIds;
import io.fd.hc2vpp.srv6.util.JvppRequestTest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SrSetEncapSource;
import io.fd.vpp.jvpp.core.dto.SrSetEncapSourceReply;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.encap.Encapsulation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.encap.EncapsulationBuilder;

public class EncapsulationSourceCustomizerTest extends JvppRequestTest implements FutureProducer {

    private static final byte[] BSID_BYTES = {0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
    private static Ipv6Address A1 = new Ipv6Address("a::1");
    private static EncapsulationSourceCustomizer sourceCustomizer;
    private static Encapsulation encapsulation = new EncapsulationBuilder()
            .setIpTtlPropagation(false)
            .setSourceAddress(A1)
            .build();

    @Captor
    private ArgumentCaptor<SrSetEncapSource> requestCaptor;

    @Override
    protected void init() {
        MockitoAnnotations.initMocks(this);
        sourceCustomizer = new EncapsulationSourceCustomizer(api);
        when(api.srSetEncapSource(requestCaptor.capture())).thenReturn(future(new SrSetEncapSourceReply()));
    }

    @Test
    public void writeCurrentAttributesTest() throws WriteFailedException {
        sourceCustomizer.writeCurrentAttributes(Srv6IIds.RT_SRV6_ENCAP, encapsulation, ctx);
        verify(api, Mockito.times(1)).srSetEncapSource(requestCaptor.capture());
        Assert.assertTrue(Arrays.equals(BSID_BYTES, requestCaptor.getValue().encapsSource));
    }

    @Test
    public void deleteCurrentAttributesTest() throws WriteFailedException {
        sourceCustomizer.deleteCurrentAttributes(Srv6IIds.RT_SRV6_ENCAP, encapsulation, ctx);
        verify(api, Mockito.times(1)).srSetEncapSource(requestCaptor.capture());
        Assert.assertNull(requestCaptor.getValue().encapsSource);
    }
}
