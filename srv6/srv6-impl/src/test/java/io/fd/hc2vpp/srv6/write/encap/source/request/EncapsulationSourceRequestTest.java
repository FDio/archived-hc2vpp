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

package io.fd.hc2vpp.srv6.write.encap.source.request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

public class EncapsulationSourceRequestTest extends JvppRequestTest {

    private static final Ipv6Address BSID = new Ipv6Address("C1::");
    private static final byte[] BSID_BYTES = {0, -63, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    @Captor
    private ArgumentCaptor<SrSetEncapSource> requestCaptor;

    @Override
    protected void init() {
        Mockito.when(api.srSetEncapSource(any())).thenReturn(future(new SrSetEncapSourceReply()));
    }

    @Test
    public void testWriteInvalid() throws WriteFailedException {
        try {
            new EncapsulationSourceWriteRequest(api).write(Srv6IIds.RT_SRV6_ENCAP);
        } catch (NullPointerException e) {
            verifyNoMoreInteractions(api);
            return;
        }
        Assert.fail("NullPointerException was expected");
    }

    @Test
    public void testWriteValid() throws WriteFailedException {
        final EncapsulationSourceWriteRequest request = new EncapsulationSourceWriteRequest(api).setBsid(BSID);

        request.write(Srv6IIds.RT_SRV6_ENCAP);
        verify(api, Mockito.times(1)).srSetEncapSource(requestCaptor.capture());
        Assert.assertEquals(BSID, request.getBsid());
        Assert.assertTrue(Arrays.equals(BSID_BYTES, requestCaptor.getValue().encapsSource));
    }

    @Test
    public void testDeleteValid() throws WriteFailedException {
        final EncapsulationSourceDeleteRequest request = new EncapsulationSourceDeleteRequest(api);

        request.delete(Srv6IIds.RT_SRV6_ENCAP);
        verify(api, Mockito.times(1)).srSetEncapSource(requestCaptor.capture());
        Assert.assertNull(requestCaptor.getValue().encapsSource);
    }
}
