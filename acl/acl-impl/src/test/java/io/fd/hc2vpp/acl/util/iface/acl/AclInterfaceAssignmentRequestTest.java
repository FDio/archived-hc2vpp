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

package io.fd.hc2vpp.acl.util.iface.acl;

import static io.fd.hc2vpp.acl.util.iface.acl.AclInterfaceAssignmentRequest.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceSetAclList;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceSetAclListReply;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Arrays;
import java.util.Collections;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AclInterfaceAssignmentRequestTest implements NamingContextHelper, FutureProducer {

    private static final String INTERFACE_NAME = "iface";
    private static final int INTERFACE_INDEX = 4;

    private static final String ACL_NAME_1 = "acl-1";
    private static final String ACL_NAME_2 = "acl-2";
    private static final String ACL_NAME_3 = "acl-3";
    private static final String ACL_NAME_4 = "acl-4";
    private static final String ACL_NAME_5 = "acl-5";

    private static final int ACL_INDEX_1 = 2;
    private static final int ACL_INDEX_2 = 7;
    private static final int ACL_INDEX_3 = 4;
    private static final int ACL_INDEX_4 = 5;
    private static final int ACL_INDEX_5 = 1;

    @Captor
    private ArgumentCaptor<AclInterfaceSetAclList> requestCaptor;

    @Mock
    private FutureJVppAclFacade api;

    @Mock
    private MappingContext mappingContext;

    private InstanceIdentifier<Acl> validIdentifier;
    private NamingContext interfaceContext;

    @Mock
    private AclContextManager aclContext;

    @Before
    public void setUp() {
        initMocks(this);

        validIdentifier = InstanceIdentifier.create(Interfaces.class).
                child(Interface.class, new InterfaceKey(INTERFACE_NAME))
                .augmentation(VppAclInterfaceAugmentation.class)
                .child(Acl.class);

        interfaceContext = new NamingContext("iface", "interface-context");

        defineMapping(mappingContext, INTERFACE_NAME, INTERFACE_INDEX, "interface-context");

        when(aclContext.getAclIndex(ACL_NAME_1, mappingContext)).thenReturn(ACL_INDEX_1);
        when(aclContext.getAclIndex(ACL_NAME_2, mappingContext)).thenReturn(ACL_INDEX_2);
        when(aclContext.getAclIndex(ACL_NAME_3, mappingContext)).thenReturn(ACL_INDEX_3);
        when(aclContext.getAclIndex(ACL_NAME_4, mappingContext)).thenReturn(ACL_INDEX_4);
        when(aclContext.getAclIndex(ACL_NAME_5, mappingContext)).thenReturn(ACL_INDEX_5);

        when(api.aclInterfaceSetAclList(any())).thenReturn(future(new AclInterfaceSetAclListReply()));
    }

    @Test
    public void verifyNegativeCases() throws WriteFailedException {
        verifyVariant(create(mappingContext));

        verifyVariant(create(mappingContext).identifier(validIdentifier));

        verifyVariant(create(mappingContext).identifier(validIdentifier).interfaceContext(interfaceContext));

        verifyVariant(create(mappingContext).identifier(validIdentifier).interfaceContext(interfaceContext)
                .standardAclContext(aclContext));

        verifyVariant(create(mappingContext).identifier(validIdentifier).interfaceContext(interfaceContext)
                .standardAclContext(aclContext).inputAclNames(Collections.emptyList()));

        verifyVariant(create(mappingContext).identifier(validIdentifier).interfaceContext(interfaceContext)
                .standardAclContext(aclContext).outputAclNames(Collections.emptyList()));
    }

    private void verifyVariant(final AclInterfaceAssignmentRequest request) throws WriteFailedException {
        verifyCreateFailsWithNullPointer(request);
        verifyUpdateFailsWithNullPointer(request);
        verifyDeleteFailsWithNullPointer(request);
    }

    @Test
    public void executeAsCreate() throws Exception {

        createValidRequest().executeAsCreate(api);
        createValidRequest().executeAsUpdate(api, mock(Acl.class), mock(Acl.class));
        createValidRequest().executeAsDelete(api);

        verify(api, times(3)).aclInterfaceSetAclList(requestCaptor.capture());
        requestCaptor.getAllValues().forEach(AclInterfaceAssignmentRequestTest::verifyValidRequest);
    }

    private AclInterfaceAssignmentRequest createValidRequest() {
        return create(mappingContext)
                .identifier(validIdentifier)
                .inputAclNames(ImmutableList.of(ACL_NAME_1, ACL_NAME_2, ACL_NAME_3))
                .outputAclNames(ImmutableList.of(ACL_NAME_4, ACL_NAME_5))
                .standardAclContext(aclContext)
                .interfaceContext(interfaceContext);
    }

    private static void verifyValidRequest(final AclInterfaceSetAclList request) {
        assertNotNull(request);
        assertEquals(5, request.count);
        assertEquals(3, request.nInput);
        assertTrue(Arrays.equals(new int[]{ACL_INDEX_1, ACL_INDEX_2, ACL_INDEX_3, ACL_INDEX_4, ACL_INDEX_5},
                request.acls));
    }

    private void verifyCreateFailsWithNullPointer(final AclInterfaceAssignmentRequest request)
            throws WriteFailedException {
        try {
            request.executeAsCreate(api);
        } catch (NullPointerException e) {
            return;
        }
        fail("Test should have thrown null pointer");
    }

    private void verifyUpdateFailsWithNullPointer(final AclInterfaceAssignmentRequest request)
            throws WriteFailedException {
        try {
            request.executeAsUpdate(api, mock(Acl.class), mock(Acl.class));
        } catch (NullPointerException e) {
            return;
        }
        fail("Test should have thrown null pointer");
    }

    private void verifyDeleteFailsWithNullPointer(final AclInterfaceAssignmentRequest request)
            throws WriteFailedException {
        try {
            request.executeAsCreate(api);
        } catch (NullPointerException e) {
            return;
        }
        fail("Test should have thrown null pointer");
    }
}