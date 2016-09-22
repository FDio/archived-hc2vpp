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

package io.fd.honeycomb.translate.v3po.interfaces;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.translate.v3po.test.ContextTestUtils;
import io.fd.honeycomb.translate.v3po.test.TestHelperUtils;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.CVlan;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.Dot1qTagVlanType;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.Dot1qVlanId;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.SVlan;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTag;
import org.opendaylight.yang.gen.v1.urn.ieee.params.xml.ns.yang.dot1q.types.rev150626.dot1q.tag.or.any.Dot1qTagBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527._802dot1ad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.match.attributes.match.type.vlan.tagged.VlanTaggedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.TagsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.tags.Tag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.tags.TagBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.tags.TagKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.CreateSubif;
import org.openvpp.jvpp.core.dto.CreateSubifReply;
import org.openvpp.jvpp.core.dto.SwInterfaceSetFlags;
import org.openvpp.jvpp.core.dto.SwInterfaceSetFlagsReply;

public class SubInterfaceCustomizerTest extends WriterCustomizerTest {

    private NamingContext namingContext;
    private SubInterfaceCustomizer customizer;

    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";
    private static final String SUPER_IF_NAME = "local0";
    private static final int SUPER_IF_ID = 1;
    private static final String SUB_IFACE_NAME = "local0.11";
    private static final int SUBIF_INDEX = 11;

    private static final short STAG_ID = 100;
    private static final short CTAG_ID = 200;
    private static final short CTAG_ANY_ID = 0; // only the *IdAny flag is set

    private final Tag STAG_100;
    private final Tag CTAG_200;
    private final Tag CTAG_ANY;

    public SubInterfaceCustomizerTest() {
        STAG_100 = generateTag((short) 0, SVlan.class, new Dot1qTag.VlanId(new Dot1qVlanId((int) STAG_ID)));
        CTAG_200 = generateTag((short) 1, CVlan.class, new Dot1qTag.VlanId(new Dot1qVlanId(200)));
        CTAG_ANY = generateTag((short) 1, CVlan.class, new Dot1qTag.VlanId(Dot1qTag.VlanId.Enumeration.Any));
    }

    @Override
    public void setUp() throws Exception {
        namingContext = new NamingContext("generatedSubInterfaceName", IFC_TEST_INSTANCE);
        customizer = new SubInterfaceCustomizer(api, namingContext);
        ContextTestUtils.mockMapping(mappingContext, SUB_IFACE_NAME, SUBIF_INDEX, IFC_TEST_INSTANCE);
        ContextTestUtils.mockMapping(mappingContext, SUPER_IF_NAME, SUPER_IF_ID, IFC_TEST_INSTANCE);
    }

    private SubInterface generateSubInterface(final boolean enabled, final List<Tag> tagList) {
        SubInterfaceBuilder builder = new SubInterfaceBuilder();
        builder.setVlanType(_802dot1ad.class);
        builder.setIdentifier(11L);
        final TagsBuilder tags = new TagsBuilder();

        tags.setTag(tagList);

        builder.setTags(tags.build());

        builder.setMatch(generateMatch());
        builder.setEnabled(enabled);
        return builder.build();
    }

    private static Tag generateTag(final short index, final Class<? extends Dot1qTagVlanType> tagType,
                                   final Dot1qTag.VlanId vlanId) {
        TagBuilder tag = new TagBuilder();
        tag.setIndex(index);
        tag.setKey(new TagKey(index));
        final Dot1qTagBuilder dtag = new Dot1qTagBuilder();
        dtag.setTagType(tagType);
        dtag.setVlanId(vlanId);
        tag.setDot1qTag(dtag.build());
        return tag.build();
    }

    private static Match generateMatch() {
        final MatchBuilder match = new MatchBuilder();
        final VlanTaggedBuilder tagged = new VlanTaggedBuilder();
        tagged.setMatchExactTags(true);
        match.setMatchType(
            new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.match.attributes.match.type.VlanTaggedBuilder()
                .setVlanTagged(tagged.build()).build());
        return match.build();
    }

    private CreateSubif generateSubInterfaceRequest(final int superIfId, final short innerVlanId,
                                                    final boolean isInnerAny) {
        CreateSubif request = new CreateSubif();
        request.subId = 11;
        request.swIfIndex = superIfId;
        request.twoTags = 1;
        request.innerVlanId = innerVlanId;
        request.innerVlanIdAny = (byte) (isInnerAny
            ? 1
            : 0);
        request.dot1Ad = 1;
        request.outerVlanId = STAG_ID;
        return request;
    }

    private SwInterfaceSetFlags generateSwInterfaceEnableRequest(final int swIfIndex) {
        SwInterfaceSetFlags request = new SwInterfaceSetFlags();
        request.swIfIndex = swIfIndex;
        request.adminUpDown = 1;
        return request;
    }

    private InstanceIdentifier<SubInterface> getSubInterfaceId(final String name, final long index) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(name)).augmentation(
            SubinterfaceAugmentation.class).child(SubInterfaces.class)
            .child(SubInterface.class, new SubInterfaceKey(index));
    }

    private void whenCreateSubifThenSuccess() throws ExecutionException, InterruptedException, VppBaseCallException {
        final CompletableFuture<CreateSubifReply> replyFuture = new CompletableFuture<>();
        final CreateSubifReply reply = new CreateSubifReply();
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).createSubif(any(CreateSubif.class));
    }

    /**
     * Failure response send
     */
    private void whenCreateSubifThenFailure() throws ExecutionException, InterruptedException, VppBaseCallException {
        doReturn(TestHelperUtils.<CreateSubifReply>createFutureException()).when(api)
            .createSubif(any(CreateSubif.class));
    }

    private void whenSwInterfaceSetFlagsThenSuccess()
        throws ExecutionException, InterruptedException, VppBaseCallException {
        final CompletableFuture<SwInterfaceSetFlagsReply> replyFuture = new CompletableFuture<>();
        final SwInterfaceSetFlagsReply reply = new SwInterfaceSetFlagsReply();
        replyFuture.complete(reply);
        doReturn(replyFuture).when(api).swInterfaceSetFlags(any(SwInterfaceSetFlags.class));
    }

    private CreateSubif verifyCreateSubifWasInvoked(final CreateSubif expected) throws VppBaseCallException {
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

    private SwInterfaceSetFlags verifySwInterfaceSetFlagsWasInvoked(final SwInterfaceSetFlags expected)
        throws VppBaseCallException {
        ArgumentCaptor<SwInterfaceSetFlags> argumentCaptor = ArgumentCaptor.forClass(SwInterfaceSetFlags.class);
        verify(api).swInterfaceSetFlags(argumentCaptor.capture());
        final SwInterfaceSetFlags actual = argumentCaptor.getValue();

        assertEquals(expected.swIfIndex, actual.swIfIndex);
        assertEquals(expected.adminUpDown, actual.adminUpDown);
        return actual;
    }

    @Test
    public void testCreateTwoTags() throws Exception {
        final SubInterface subInterface = generateSubInterface(false, Arrays.asList(STAG_100, CTAG_200));
        final InstanceIdentifier<SubInterface> id = getSubInterfaceId(SUPER_IF_NAME, SUBIF_INDEX);

        whenCreateSubifThenSuccess();
        whenSwInterfaceSetFlagsThenSuccess();

        customizer.writeCurrentAttributes(id, subInterface, writeContext);

        verifyCreateSubifWasInvoked(generateSubInterfaceRequest(SUPER_IF_ID, CTAG_ID, false));
        verify(mappingContext)
            .put(eq(ContextTestUtils.getMappingIid(SUB_IFACE_NAME, IFC_TEST_INSTANCE)), eq(
                    ContextTestUtils.getMapping(SUB_IFACE_NAME, 0).get()));
    }

    @Test
    public void testCreateDot1qAnyTag() throws Exception {
        final SubInterface subInterface = generateSubInterface(false, Arrays.asList(STAG_100, CTAG_ANY));
        final InstanceIdentifier<SubInterface> id = getSubInterfaceId(SUPER_IF_NAME, SUBIF_INDEX);

        whenCreateSubifThenSuccess();
        whenSwInterfaceSetFlagsThenSuccess();

        customizer.writeCurrentAttributes(id, subInterface, writeContext);

        verifyCreateSubifWasInvoked(generateSubInterfaceRequest(SUPER_IF_ID, CTAG_ANY_ID, true));
        verify(mappingContext)
            .put(eq(ContextTestUtils.getMappingIid(SUB_IFACE_NAME, IFC_TEST_INSTANCE)), eq(
                    ContextTestUtils.getMapping(SUB_IFACE_NAME, 0).get()));
    }

    @Test
    public void testCreateFailed() throws Exception {
        final SubInterface subInterface = generateSubInterface(false, Arrays.asList(STAG_100, CTAG_200));
        final InstanceIdentifier<SubInterface> id = getSubInterfaceId(SUPER_IF_NAME, SUBIF_INDEX);

        whenCreateSubifThenFailure();

        try {
            customizer.writeCurrentAttributes(id, subInterface, writeContext);
        } catch (WriteFailedException.CreateFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verifyCreateSubifWasInvoked(generateSubInterfaceRequest(SUPER_IF_ID, CTAG_ID, false));
            verify(mappingContext, times(0)).put(
                eq(ContextTestUtils.getMappingIid(SUPER_IF_NAME, IFC_TEST_INSTANCE)),
                eq(ContextTestUtils.getMapping(SUPER_IF_NAME, 0).get()));
            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testUpdate() throws Exception {
        final List<Tag> tags = Arrays.asList(STAG_100, CTAG_200);
        final SubInterface before = generateSubInterface(false, tags);
        final SubInterface after = generateSubInterface(true, tags);
        final InstanceIdentifier<SubInterface> id = getSubInterfaceId(SUPER_IF_NAME, SUBIF_INDEX);

        whenSwInterfaceSetFlagsThenSuccess();
        customizer.updateCurrentAttributes(id, before, after, writeContext);

        verifySwInterfaceSetFlagsWasInvoked(generateSwInterfaceEnableRequest(SUBIF_INDEX));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDelete() throws Exception {
        final SubInterface subInterface = generateSubInterface(false, Arrays.asList(STAG_100, CTAG_200));
        customizer.deleteCurrentAttributes(null, subInterface, writeContext);
    }
}