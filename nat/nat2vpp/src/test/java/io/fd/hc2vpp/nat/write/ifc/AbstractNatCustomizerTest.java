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

package io.fd.hc2vpp.nat.write.ifc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.nat.dto.Nat44InterfaceAddDelFeature;
import io.fd.jvpp.nat.dto.Nat44InterfaceAddDelFeatureReply;
import io.fd.jvpp.nat.dto.Nat44InterfaceAddDelOutputFeature;
import io.fd.jvpp.nat.dto.Nat44InterfaceAddDelOutputFeatureReply;
import io.fd.jvpp.nat.dto.Nat64AddDelInterface;
import io.fd.jvpp.nat.dto.Nat64AddDelInterfaceReply;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816.InterfaceNatVppFeatureAttributes;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev170816._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractNatCustomizerTest<D extends InterfaceNatVppFeatureAttributes & DataObject, T extends AbstractInterfaceNatCustomizer<D>>
        extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;
    private T customizer;

    @Mock
    private FutureJVppNatFacade natApi;
    private NamingContext ifcNamingCtx = new NamingContext("generatedIfaceName", IFC_CTX_NAME);

    @Override
    public void setUpTest() {
        customizer = getCustomizer(natApi, ifcNamingCtx);
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        when(natApi.nat44InterfaceAddDelFeature(any())).thenReturn(future(new Nat44InterfaceAddDelFeatureReply()));
        when(natApi.nat44InterfaceAddDelOutputFeature(any()))
                .thenReturn(future(new Nat44InterfaceAddDelOutputFeatureReply()));
        when(natApi.nat64AddDelInterface(any())).thenReturn(future(new Nat64AddDelInterfaceReply()));
    }

    @Test
    public void testWritePreRouting() throws Exception {
        final D data = getPreRoutingConfig();
        customizer.writeCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verifyPreRouting(data, true);
    }

    @Test
    public void testWritePostRouting() throws Exception {
        final D data = getPostRoutingConfig();
        customizer.writeCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verify(natApi).nat44InterfaceAddDelOutputFeature(expectedPostRoutingRequest(data, true));
        verify(natApi, never()).nat64AddDelInterface(any()); // VPP does not support it currently
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdatePreRouting() throws Exception {
        customizer.updateCurrentAttributes(getIId(IFACE_NAME), getPreRoutingConfig(), getPreRoutingConfig(),
                writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdatePostRouting() throws Exception {
        customizer.updateCurrentAttributes(getIId(IFACE_NAME), getPostRoutingConfig(), getPostRoutingConfig(),
                writeContext);
    }

    @Test
    public void testDeletePreRouting() throws Exception {
        final D data = getPreRoutingConfig();
        customizer.deleteCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verifyPreRouting(data, false);
    }

    @Test
    public void testDeletePostRouting() throws Exception {
        final D data = getPostRoutingConfig();
        customizer.deleteCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verify(natApi).nat44InterfaceAddDelOutputFeature(expectedPostRoutingRequest(data, false));
        verify(natApi, never()).nat64AddDelInterface(any()); // VPP does not support it currently
    }

    private void verifyPreRouting(final D data, final boolean isAdd) {
        if (data.isNat44Support()) {
            verify(natApi).nat44InterfaceAddDelFeature(expectedPreRoutingNat44Request(data, isAdd));
        } else {
            verify(natApi, never()).nat44InterfaceAddDelFeature(any());
        }
        if (data.isNat64Support() != null && data.isNat64Support()) {
            verify(natApi).nat64AddDelInterface(expectedPreRoutingNat64Request(data, isAdd));
        } else {
            verify(natApi, never()).nat64AddDelInterface(any());
        }

    }

    private Nat44InterfaceAddDelFeature expectedPreRoutingNat44Request(final D data, boolean isAdd) {
        Nat44InterfaceAddDelFeature request = new Nat44InterfaceAddDelFeature();
        request.isInside = booleanToByte(data instanceof Inbound);
        request.swIfIndex = IFACE_ID;
        request.isAdd = booleanToByte(isAdd);
        return request;
    }

    private Nat64AddDelInterface expectedPreRoutingNat64Request(final D data, boolean isAdd) {
        Nat64AddDelInterface request = new Nat64AddDelInterface();
        request.isInside = booleanToByte(data instanceof Inbound);
        request.swIfIndex = IFACE_ID;
        request.isAdd = booleanToByte(isAdd);
        return request;
    }

    private Nat44InterfaceAddDelOutputFeature expectedPostRoutingRequest(final D data, boolean isAdd) {
        Nat44InterfaceAddDelOutputFeature request = new Nat44InterfaceAddDelOutputFeature();
        request.isInside = booleanToByte(data instanceof Inbound);
        request.swIfIndex = IFACE_ID;
        request.isAdd = booleanToByte(isAdd);
        return request;
    }

    protected abstract D getPreRoutingConfig();

    protected abstract D getPostRoutingConfig();

    protected abstract InstanceIdentifier<D> getIId(final String ifaceName);

    protected abstract T getCustomizer(final FutureJVppNatFacade natApi, final NamingContext ifcNamingCtx);
}
