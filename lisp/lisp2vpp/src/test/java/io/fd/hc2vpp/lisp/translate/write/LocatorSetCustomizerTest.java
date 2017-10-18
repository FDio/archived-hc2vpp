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

package io.fd.hc2vpp.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneAddDelLocatorSet;
import io.fd.vpp.jvpp.core.dto.OneAddDelLocatorSetReply;
import io.fd.vpp.jvpp.core.dto.OneLocatorSetDetails;
import io.fd.vpp.jvpp.core.dto.OneLocatorSetDetailsReplyDump;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.LocalMappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.EidTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.locator.sets.grouping.locator.sets.locator.set.InterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocatorSetCustomizerTest extends LispWriterCustomizerTest {

    private static final InstanceIdentifier<EidTable>
            EID_TABLE_ID = InstanceIdentifier.create(Lisp.class)
            .child(LispFeatureData.class)
            .child(EidTable.class);

    private static final LocatorSet LOCATOR_SET_TO_DELETE = new LocatorSetBuilder()
            .setName("Locator")
            .build();

    private LocatorSetCustomizer customizer;

    private final InstanceIdentifier<LocatorSet> EMPTY_ID = InstanceIdentifier.create(LocatorSet.class);
    private final LocatorSet EMPTY_DATA = new LocatorSetBuilder().build();

    @Override
    public void setUpTest() {
        customizer = new LocatorSetCustomizer(api, new NamingContext("locator-set", "locator-set-context"),
                lispStateCheckService);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, null, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesBadData() throws WriteFailedException {
        customizer.writeCurrentAttributes(null, mock(LocatorSet.class), writeContext);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {
        noMappingDefined(mappingContext, "Locator", "locator-set-context");
        LocatorSet locatorSet = new LocatorSetBuilder()
                .setName("Locator")
                .setInterface(Arrays.asList(new InterfaceBuilder().build()))
                .build();

        InstanceIdentifier<LocatorSet> validId =
                InstanceIdentifier.create(LocatorSets.class).child(LocatorSet.class, new LocatorSetKey("Locator"));


        ArgumentCaptor<OneAddDelLocatorSet> locatorSetCaptor = ArgumentCaptor.forClass(OneAddDelLocatorSet.class);

        when(api.oneAddDelLocatorSet(any(OneAddDelLocatorSet.class)))
                .thenReturn(future(new OneAddDelLocatorSetReply()));
        when(writeContext.readAfter(validId)).thenReturn(Optional.of(locatorSet));

        final OneLocatorSetDetailsReplyDump reply = new OneLocatorSetDetailsReplyDump();
        OneLocatorSetDetails details = new OneLocatorSetDetails();
        details.lsName = "Locator".getBytes(StandardCharsets.UTF_8);
        reply.oneLocatorSetDetails = ImmutableList.of(details);

        customizer.writeCurrentAttributes(validId, locatorSet, writeContext);

        verify(api, times(1)).oneAddDelLocatorSet(locatorSetCaptor.capture());

        OneAddDelLocatorSet request = locatorSetCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals(1, request.isAdd);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        final InstanceIdentifier<LocatorSet> identifier = InstanceIdentifier.create(LocatorSet.class);
        customizer.updateCurrentAttributes(identifier, mock(LocatorSet.class), mock(LocatorSet.class), writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        customizer.deleteCurrentAttributes(null, null, writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesBadData() throws WriteFailedException {
        customizer.deleteCurrentAttributes(null, mock(LocatorSet.class), writeContext);
    }

    @Test
    public void testDeleteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {
        when(writeContext.readAfter(EID_TABLE_ID)).thenReturn(Optional.absent());
        verifySuccessfullDelete(LOCATOR_SET_TO_DELETE);
    }

    @Test
    public void testDeleteCurrentAttributesWithoutLocalMappingContainer()
            throws InterruptedException, ExecutionException, WriteFailedException {
        when(writeContext.readAfter(EID_TABLE_ID)).thenReturn(eidTableDataWithoutLocalMappingContainer());
        verifySuccessfullDelete(LOCATOR_SET_TO_DELETE);
    }

    @Test
    public void testDeleteCurrentAttributesWithoutLocalMappingValues()
            throws InterruptedException, ExecutionException, WriteFailedException {
        when(writeContext.readAfter(EID_TABLE_ID)).thenReturn(eidTableDataWithoutLocalMappingValues());
        verifySuccessfullDelete(LOCATOR_SET_TO_DELETE);
    }

    private void verifySuccessfullDelete(final LocatorSet locatorSet) throws WriteFailedException {
        ArgumentCaptor<OneAddDelLocatorSet> locatorSetCaptor = ArgumentCaptor.forClass(OneAddDelLocatorSet.class);

        when(api.oneAddDelLocatorSet(any(OneAddDelLocatorSet.class)))
                .thenReturn(future(new OneAddDelLocatorSetReply()));

        customizer.deleteCurrentAttributes(null, locatorSet, writeContext);

        verify(api, times(1)).oneAddDelLocatorSet(locatorSetCaptor.capture());

        OneAddDelLocatorSet request = locatorSetCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals(0, request.isAdd);
    }

    @Test
    public void testDeleteReferenced() throws InterruptedException, ExecutionException, WriteFailedException {
        when(writeContext.readAfter(EID_TABLE_ID))
                .thenReturn(eidTableData());

        ArgumentCaptor<OneAddDelLocatorSet> locatorSetCaptor = ArgumentCaptor.forClass(OneAddDelLocatorSet.class);

        when(api.oneAddDelLocatorSet(any(OneAddDelLocatorSet.class)))
                .thenReturn(future(new OneAddDelLocatorSetReply()));

        try {
            customizer.deleteCurrentAttributes(null, LOCATOR_SET_TO_DELETE, writeContext);
        } catch (IllegalStateException e) {
            verify(api, times(0)).oneAddDelLocatorSet(locatorSetCaptor.capture());
            return;
        }
        fail("testDeleteReferenced should have failed");
    }

    @Test
    public void testWriteLispDisabled() throws WriteFailedException {
        mockLispDisabledAfter();
        try {
            customizer.writeCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    @Test
    public void testDeleteLispDisabled() throws WriteFailedException {
        mockLispDisabledBefore();
        try {
            customizer.deleteCurrentAttributes(EMPTY_ID, EMPTY_DATA, writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have thrown IllegalArgumentException");
    }

    private static Optional<EidTable> eidTableData() {
        return Optional.of(new EidTableBuilder()
                .setVniTable(
                        Arrays.asList(new VniTableBuilder()
                                .setVrfSubtable(new VrfSubtableBuilder()
                                        .setLocalMappings(new LocalMappingsBuilder()
                                                .setLocalMapping(Arrays.asList(
                                                        new LocalMappingBuilder().setLocatorSet("Locator")
                                                                .build(),
                                                        new LocalMappingBuilder()
                                                                .setLocatorSet("OtherLocatorSet").build()
                                                )).build()).build()).build())).build());
    }

    private static Optional<EidTable> eidTableDataWithoutLocalMappingValues() {
        return Optional.of(new EidTableBuilder()
                .setVniTable(
                        Arrays.asList(new VniTableBuilder()
                                .setVrfSubtable(new VrfSubtableBuilder()
                                        .setLocalMappings(new LocalMappingsBuilder().build()).build()).build()))
                .build());
    }

    private static Optional<EidTable> eidTableDataWithoutLocalMappingContainer() {
        return Optional.of(new EidTableBuilder()
                .setVniTable(
                        Arrays.asList(new VniTableBuilder().setVrfSubtable(new VrfSubtableBuilder().build()).build()))
                .build());
    }


}
