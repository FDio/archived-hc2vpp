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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import static io.fd.honeycomb.v3po.translate.v3po.ContextTestUtils.getMapping;
import static io.fd.honeycomb.v3po.translate.v3po.ContextTestUtils.getMappingIid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanTag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VlanType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.SubInterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.CreateSubif;
import org.openvpp.jvpp.dto.CreateSubifReply;
import org.openvpp.jvpp.future.FutureJVpp;

public class SubInterfaceCustomizerTest {

    @Mock
    private FutureJVpp api;
    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;

    private NamingContext namingContext;
    private SubInterfaceCustomizer customizer;
    public static final String SUPER_IF_NAME = "local0";
    public static final int SUPER_IF_ID = 1;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        InterfaceTypeTestUtils.setupWriteContext(writeContext,
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.SubInterface.class);
        namingContext = new NamingContext("generatedSubInterfaceName", "test-instance");
        doReturn(mappingContext).when(writeContext).getMappingContext();
        // TODO create base class for tests using vppApi
        customizer = new SubInterfaceCustomizer(api, namingContext);
        doReturn(getMapping(SUPER_IF_NAME, SUPER_IF_ID)).when(mappingContext).read(getMappingIid(SUPER_IF_NAME, "test-instance"));
    }

    private SubInterface generateSubInterface(final String superIfName) {
        SubInterfaceBuilder builder = new SubInterfaceBuilder();
        builder.setVlanType(VlanType._802dot1ad);
        builder.setIdentifier(11L);
        builder.setNumberOfTags((short)1);
        builder.setOuterId(new VlanTag(100));
        builder.setInnerId(new VlanTag(200));
        builder.setSuperInterface(superIfName);
        return builder.build();
    }

    private CreateSubif generateSubInterfaceRequest(final int superIfId) {
        CreateSubif request = new CreateSubif();
        request.subId = 11;
        request.swIfIndex = superIfId;
        request.oneTag = 1;
        request.dot1Ad = 1;
        request.outerVlanId = 100;
        request.innerVlanId = 200;
        return request;
    }

    private InstanceIdentifier<SubInterface> getSubInterfaceId(final String name) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(name)).augmentation(
                VppInterfaceAugmentation.class).child(SubInterface.class);
    }

    private void whenCreateSubifThen(final int retval) throws ExecutionException, InterruptedException {
        final CompletableFuture<CreateSubifReply> replyFuture = new CompletableFuture<>();
        final CreateSubifReply reply = new CreateSubifReply();
        reply.retval = retval;
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).createSubif(any(CreateSubif.class));
    }

    private void whenCreateSubifThenSuccess() throws ExecutionException, InterruptedException {
        whenCreateSubifThen(0);
    }

    private void whenCreateSubifThenFailure() throws ExecutionException, InterruptedException {
        whenCreateSubifThen(-1);
    }

    private CreateSubif verifyCreateSubifWasInvoked(final CreateSubif expected) {
        ArgumentCaptor<CreateSubif> argumentCaptor = ArgumentCaptor.forClass(CreateSubif.class);
        verify(api).createSubif(argumentCaptor.capture());
        final CreateSubif actual = argumentCaptor.getValue();

        assertEquals(expected.swIfIndex, actual.swIfIndex);
        assertEquals(expected.subId, actual.subId);
        assertEquals(expected.noTags, actual.noTags);
        assertEquals(expected.oneTag, actual.oneTag);
        assertEquals(expected.twoTags, actual.twoTags);
        assertEquals(expected.dot1Ad, actual.dot1Ad);
        assertEquals(expected.exactMatch, actual.exactMatch);
        assertEquals(expected.defaultSub, actual.defaultSub);
        assertEquals(expected.outerVlanIdAny, actual.outerVlanIdAny);
        assertEquals(expected.innerVlanIdAny, actual.innerVlanIdAny);
        assertEquals(expected.outerVlanId, actual.outerVlanId);
        assertEquals(expected.innerVlanId, actual.innerVlanId);
        return actual;
    }

    @Test
    public void testCreate() throws Exception {
        final SubInterface subInterface = generateSubInterface(SUPER_IF_NAME);
        final String subIfaceName = "local0.sub1";
        final InstanceIdentifier<SubInterface> id = getSubInterfaceId(subIfaceName);

        whenCreateSubifThenSuccess();

        customizer.writeCurrentAttributes(id, subInterface, writeContext);

        verifyCreateSubifWasInvoked(generateSubInterfaceRequest(SUPER_IF_ID));
        verify(mappingContext).put(eq(getMappingIid(subIfaceName, "test-instance")), eq(getMapping(subIfaceName, 0).get()));
    }

    @Test
    public void testCreateFailed() throws Exception {
        final SubInterface subInterface = generateSubInterface(SUPER_IF_NAME);
        final String subIfaceName = "local0.sub1";
        final InstanceIdentifier<SubInterface> id = getSubInterfaceId(subIfaceName);

        whenCreateSubifThenFailure();

        try {
            customizer.writeCurrentAttributes(id, subInterface, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertEquals(VppApiInvocationException.class, e.getCause().getClass());
            verifyCreateSubifWasInvoked(generateSubInterfaceRequest(SUPER_IF_ID));
            verify(mappingContext, times(0)).put(
                eq(getMappingIid(subIfaceName, "test-instance")),
                eq(getMapping(subIfaceName, 0).get()));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testUpdateNoChange() throws Exception {
        final SubInterface before = generateSubInterface(SUPER_IF_NAME);
        final SubInterface after = generateSubInterface(SUPER_IF_NAME);
        customizer.updateCurrentAttributes(null, before, after, writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws Exception {
        final SubInterface before = generateSubInterface("eth0");
        final SubInterface after = generateSubInterface("eth1");
        customizer.updateCurrentAttributes(null, before, after, writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDelete() throws Exception {
        final SubInterface subInterface = generateSubInterface("eth0");
        customizer.deleteCurrentAttributes(null, subInterface, writeContext);
    }
}