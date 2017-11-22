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

package io.fd.hc2vpp.mpls;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsTableAddDel;
import io.fd.vpp.jvpp.core.dto.MplsTableAddDelReply;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetMplsEnable;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetMplsEnableReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCoreFacade;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.Routing1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls._interface.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.routing.Mpls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MplsInterfaceCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 123;
    private static final Interface MPLS_ENABLED = getInterfaceMpls(true);
    private static final Interface MPLS_DISABLED = getInterfaceMpls(false);
    private static final InstanceIdentifier<Mpls> MPLS_ID = InstanceIdentifier.create(Routing.class).augmentation
        (Routing1.class).child(Mpls.class);
    private static final InstanceIdentifier<Interface> IID = MPLS_ID.child(Interface.class, new InterfaceKey(IF_NAME));

    @Mock
    private FutureJVppCoreFacade jvpp;
    private MplsInterfaceCustomizer customizer;

    private static Interface getInterfaceMpls(final boolean enabled) {
        return new InterfaceBuilder()
            .setName(IF_NAME)
            .setConfig(new ConfigBuilder()
                .setEnabled(enabled)
                .build())
            .build();
    }

    @Override
    public void setUpTest() {
        final String ifcCtxName = "ifc-test-instance";
        final NamingContext ifcContext = new NamingContext("generatedIfaceName", ifcCtxName);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, ifcCtxName);
        customizer = new MplsInterfaceCustomizer(jvpp, ifcContext);
        when(jvpp.swInterfaceSetMplsEnable(any())).thenReturn(future(new SwInterfaceSetMplsEnableReply()));
        when(jvpp.mplsTableAddDel(any())).thenReturn(future(new MplsTableAddDelReply()));
    }

    @Test
    public void testWrite() throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, MPLS_ENABLED, writeContext);
        verify(jvpp).mplsTableAddDel(getMplsTableRequest());
        verify(jvpp).swInterfaceSetMplsEnable(getInterfaceMplsRequest(true));
    }

    @Test
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, MPLS_ENABLED, MPLS_DISABLED, writeContext);
        verify(jvpp).mplsTableAddDel(getMplsTableRequest());
        verify(jvpp).swInterfaceSetMplsEnable(getInterfaceMplsRequest(false));
    }

    @Test
    public void testDelete() throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, MPLS_ENABLED, writeContext);
        verify(jvpp).swInterfaceSetMplsEnable(getInterfaceMplsRequest(false));
    }

    private MplsTableAddDel getMplsTableRequest() {
        final MplsTableAddDel request = new MplsTableAddDel();
        request.mtIsAdd = 1;
        request.mtTableId = 0;
        request.mtName = new byte[0];
        return request;
    }

    private SwInterfaceSetMplsEnable getInterfaceMplsRequest(final boolean enable) {
        final SwInterfaceSetMplsEnable request = new SwInterfaceSetMplsEnable();
        request.enable = booleanToByte(enable);
        request.swIfIndex = IF_INDEX;
        return request;
    }
}