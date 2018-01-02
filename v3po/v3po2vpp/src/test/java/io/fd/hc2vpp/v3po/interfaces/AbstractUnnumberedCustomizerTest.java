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

package io.fd.hc2vpp.v3po.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetUnnumbered;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetUnnumberedReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.UnnumberedBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractUnnumberedCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    protected static final String IFC_CTX_NAME = "ifc-ctx";
    private AbstractUnnumberedCustomizer customizer;
    private static final String TARGET_IFC0_NAME = "eth0";
    private static final int TARGET_IFC0_ID = 0;
    private static final String TARGET_IFC1_NAME = "eth1";
    private static final int TARGET_IFC1_ID = 1;


    @Override
    public void setUpTest() {
        customizer = getCustomizer();
        defineMapping(mappingContext, TARGET_IFC0_NAME, TARGET_IFC0_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, TARGET_IFC1_NAME, TARGET_IFC1_ID, IFC_CTX_NAME);
        defineMapping(mappingContext, getUnnumberedIfcName(), getUnnumberedIfcId(), IFC_CTX_NAME);
        when(api.swInterfaceSetUnnumbered(any())).thenReturn(future(new SwInterfaceSetUnnumberedReply()));
    }

    protected abstract int getUnnumberedIfcId();

    protected abstract String getUnnumberedIfcName();

    protected abstract InstanceIdentifier<Unnumbered> getUnnumberedIfcIId();

    protected abstract AbstractUnnumberedCustomizer getCustomizer();

    @Test
    public void testWrite() throws Exception {
        final Unnumbered data = new UnnumberedBuilder().setUse(TARGET_IFC0_NAME).build();
        customizer.writeCurrentAttributes(getUnnumberedIfcIId(), data, writeContext);
        verify(api).swInterfaceSetUnnumbered(expectedRequest(true, TARGET_IFC0_ID));
    }
    @Test
    public void testUpdate() throws Exception {
        final Unnumbered before = new UnnumberedBuilder().setUse(TARGET_IFC0_NAME).build();
        final Unnumbered after = new UnnumberedBuilder().setUse(TARGET_IFC1_NAME).build();
        customizer.updateCurrentAttributes(getUnnumberedIfcIId(), before, after, writeContext);
        verify(api).swInterfaceSetUnnumbered(expectedRequest(true, TARGET_IFC1_ID));
    }

    @Test
    public void testDelete() throws Exception {
        final Unnumbered data = new UnnumberedBuilder().setUse(TARGET_IFC0_NAME).build();
        customizer.deleteCurrentAttributes(getUnnumberedIfcIId(), data, writeContext);
        verify(api).swInterfaceSetUnnumbered(expectedRequest(false, TARGET_IFC0_ID));
    }

    private SwInterfaceSetUnnumbered expectedRequest(final boolean isAdd, int swIfIntex) {
        final SwInterfaceSetUnnumbered request = new SwInterfaceSetUnnumbered();
        request.swIfIndex = swIfIntex;
        request.unnumberedSwIfIndex = getUnnumberedIfcId();
        request.isAdd = booleanToByte(isAdd);
        return request;
    }
}