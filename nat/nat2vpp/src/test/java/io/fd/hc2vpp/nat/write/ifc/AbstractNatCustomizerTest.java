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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelFeature;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelFeatureReply;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelOutputFeature;
import io.fd.vpp.jvpp.snat.dto.SnatInterfaceAddDelOutputFeatureReply;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170801.InterfaceNatVppFeatureAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev170801._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractNatCustomizerTest<D extends InterfaceNatVppFeatureAttributes & DataObject, T extends AbstractInterfaceNatCustomizer<D>>
        extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IFACE_NAME = "eth0";
    private static final int IFACE_ID = 123;
    private T customizer;

    @Mock
    private FutureJVppSnatFacade snatApi;
    private NamingContext ifcNamingCtx = new NamingContext("generatedIfaceName", IFC_CTX_NAME);

    @Override
    public void setUpTest() {
        customizer = getCustomizer(snatApi, ifcNamingCtx);
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        when(snatApi.snatInterfaceAddDelFeature(any())).thenReturn(future(new SnatInterfaceAddDelFeatureReply()));
        when(snatApi.snatInterfaceAddDelOutputFeature(any()))
                .thenReturn(future(new SnatInterfaceAddDelOutputFeatureReply()));
    }

    @Test
    public void testWritePreRouting() throws Exception {
        final D data = getPreRoutingConfig();
        customizer.writeCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verify(snatApi).snatInterfaceAddDelFeature(expectedPreRoutingRequest(data, true));
    }

    @Test
    public void testWritePostRouting() throws Exception {
        final D data = getPostRoutingConfig();
        customizer.writeCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verify(snatApi).snatInterfaceAddDelOutputFeature(expectedPostRoutingRequest(data, true));
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdatePreRouting() throws Exception {
        customizer.updateCurrentAttributes(getIId(IFACE_NAME), getPreRoutingConfig(), getPreRoutingConfig(),
                writeContext);
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdatePostRouting() throws Exception {
        customizer.updateCurrentAttributes(getIId(IFACE_NAME), getPostRoutingConfig(), getPostRoutingConfig(),
                writeContext);
    }

    @Test
    public void testDeletePreRouting() throws Exception {
        final D data = getPreRoutingConfig();
        customizer.deleteCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verify(snatApi).snatInterfaceAddDelFeature(expectedPreRoutingRequest(data, false));
    }

    @Test
    public void testDeletePostRouting() throws Exception {
        final D data = getPostRoutingConfig();
        customizer.deleteCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verify(snatApi).snatInterfaceAddDelOutputFeature(expectedPostRoutingRequest(data, false));
    }

    private SnatInterfaceAddDelFeature expectedPreRoutingRequest(final D data, boolean isAdd) {
        SnatInterfaceAddDelFeature request = new SnatInterfaceAddDelFeature();
        request.isInside = (byte) ((data instanceof Inbound)
                ? 1
                : 0);
        request.swIfIndex = IFACE_ID;
        request.isAdd = booleanToByte(isAdd);
        return request;
    }

    private SnatInterfaceAddDelOutputFeature expectedPostRoutingRequest(final D data, boolean isAdd) {
        SnatInterfaceAddDelOutputFeature request = new SnatInterfaceAddDelOutputFeature();
        request.isInside = (byte) ((data instanceof Inbound)
                ? 1
                : 0);
        request.swIfIndex = IFACE_ID;
        request.isAdd = booleanToByte(isAdd);
        return request;
    }

    protected abstract D getPreRoutingConfig();

    protected abstract D getPostRoutingConfig();

    protected abstract InstanceIdentifier<D> getIId(final String ifaceName);

    protected abstract T getCustomizer(final FutureJVppSnatFacade snatApi, final NamingContext ifcNamingCtx);
}