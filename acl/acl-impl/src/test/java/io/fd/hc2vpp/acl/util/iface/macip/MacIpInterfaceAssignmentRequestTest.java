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

package io.fd.hc2vpp.acl.util.iface.macip;

import static io.fd.hc2vpp.acl.util.iface.macip.MacIpInterfaceAssignmentRequest.addNew;
import static io.fd.hc2vpp.acl.util.iface.macip.MacIpInterfaceAssignmentRequest.deleteExisting;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceAddDel;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceAddDelReply;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Created by jsrnicek on 13.12.2016.
 */
public class MacIpInterfaceAssignmentRequestTest implements NamingContextHelper,FutureProducer {

    private static final String INTERFACE_NAME = "iface";
    private static final int INTERFACE_INDEX = 4;
    private static final String ACL_NAME = "mac-ip-acl";
    private static final int ACL_INDEX = 5;

    @Captor
    private ArgumentCaptor<MacipAclInterfaceAddDel> requestCaptor;

    @Mock
    private FutureJVppAclFacade api;

    @Mock
    private MappingContext mappingContext;

    private InstanceIdentifier<VppMacipAcl> validIdentifier;
    private NamingContext interfaceContext;
    private NamingContext macIpAclContext;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        validIdentifier = InstanceIdentifier.create(Interfaces.class).
                child(Interface.class, new InterfaceKey(INTERFACE_NAME))
                .augmentation(VppAclInterfaceAugmentation.class)
                .child(Acl.class)
                .child(Ingress.class)
                .child(VppMacipAcl.class);

        interfaceContext = new NamingContext("iface", "interface-context");
        macIpAclContext = new NamingContext("mac-ip-acl", "mac-ip-acl-context");

        defineMapping(mappingContext, INTERFACE_NAME, INTERFACE_INDEX, "interface-context");
        defineMapping(mappingContext, ACL_NAME, ACL_INDEX, "mac-ip-acl-context");
        when(api.macipAclInterfaceAddDel(any())).thenReturn(future(new MacipAclInterfaceAddDelReply()));
    }

    @Test
    public void testAddNew() throws Exception {
        addNew(mappingContext)
                .aclName(ACL_NAME)
                .identifier(validIdentifier)
                .interfaceContext(interfaceContext)
                .macIpAclContext(macIpAclContext)
                .execute(api);
        verify(api, times(1)).macipAclInterfaceAddDel(requestCaptor.capture());

        final MacipAclInterfaceAddDel request = requestCaptor.getValue();

        assertNotNull(request);
        assertEquals(1, request.isAdd);
        assertEquals(INTERFACE_INDEX, request.swIfIndex);
        assertEquals(ACL_INDEX, request.aclIndex);
    }

    @Test
    public void testDeleteExisting() throws Exception {
        deleteExisting(mappingContext)
                .aclName(ACL_NAME)
                .identifier(validIdentifier)
                .interfaceContext(interfaceContext)
                .macIpAclContext(macIpAclContext)
                .execute(api);
        verify(api, times(1)).macipAclInterfaceAddDel(requestCaptor.capture());

        final MacipAclInterfaceAddDel request = requestCaptor.getValue();

        assertNotNull(request);
        assertEquals(0, request.isAdd);
        assertEquals(INTERFACE_INDEX, request.swIfIndex);
        assertEquals(ACL_INDEX, request.aclIndex);
    }

    @Test
    public void testInvalidCases() throws Exception {
        verifyFailsWithNullPointer(addNew(mappingContext));
        verifyFailsWithNullPointer(addNew(mappingContext).aclName(ACL_NAME));
        verifyFailsWithNullPointer(addNew(mappingContext).aclName(ACL_NAME).interfaceContext(interfaceContext));
        verifyFailsWithNullPointer(addNew(mappingContext).aclName(ACL_NAME).interfaceContext(interfaceContext)
                .macIpAclContext(macIpAclContext));
        verifyFailsWithNullPointer(addNew(mappingContext).aclName(ACL_NAME).interfaceContext(interfaceContext)
                .identifier(validIdentifier));
    }

    private void verifyFailsWithNullPointer(final MacIpInterfaceAssignmentRequest request) throws WriteFailedException {
        try {
            request.execute(api);
        } catch (NullPointerException e) {
            return;
        }
        fail("Test should have thrown null pointer");
    }
}