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
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.nat.rev161214._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractNatCustomizerTest<D extends DataObject, T extends AbstractInterfaceNatCustomizer<D>>
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
    }

    @Test
    public void testWrite() throws Exception {
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        when(snatApi.snatInterfaceAddDelFeature(any())).thenReturn(future(new SnatInterfaceAddDelFeatureReply()));
        final D data = getData();
        customizer.writeCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verify(snatApi).snatInterfaceAddDelFeature(expectedRequest(data, true));
    }

    @Test(expected = WriteFailedException.UpdateFailedException.class)
    public void testUpdate() throws Exception {
        customizer.updateCurrentAttributes(getIId(IFACE_NAME), getData(), getData(), writeContext);
    }

    @Test
    public void testDelete() throws Exception {
        defineMapping(mappingContext, IFACE_NAME, IFACE_ID, IFC_CTX_NAME);
        when(snatApi.snatInterfaceAddDelFeature(any())).thenReturn(future(new SnatInterfaceAddDelFeatureReply()));
        final D data = getData();
        customizer.deleteCurrentAttributes(getIId(IFACE_NAME), data, writeContext);
        verify(snatApi).snatInterfaceAddDelFeature(expectedRequest(data, false));
    }

    private SnatInterfaceAddDelFeature expectedRequest(final D data, boolean isAdd) {
        SnatInterfaceAddDelFeature request = new SnatInterfaceAddDelFeature();
        request.isInside = (byte) ((data instanceof Inbound) ? 1 : 0);
        request.swIfIndex = IFACE_ID;
        request.isAdd = booleanToByte(isAdd);
        return request;
    }

    protected abstract D getData();

    protected abstract InstanceIdentifier<D> getIId(final String ifaceName);

    protected abstract T getCustomizer(final FutureJVppSnatFacade snatApi, final NamingContext ifcNamingCtx);
}