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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckService;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.vpp.jvpp.core.dto.GpeAddDelFwdEntry;
import io.fd.vpp.jvpp.core.dto.GpeAddDelFwdEntryReply;
import io.fd.vpp.jvpp.core.types.GpeLocator;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.GpeEntryTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.$YangModuleInfoImpl;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class GpeForwardEntryCustomizerTest extends WriterCustomizerTest
        implements InjectablesProcessor, ByteDataTranslator {

    private static final String GPE_ENTRY_ID = "gpe-fwd-entry-1";
    private static final String GPE_ENTRY_PATH = "/gpe:gpe" +
            "/gpe:gpe-feature-data" +
            "/gpe:gpe-entry-table";
    private static final byte[] LOCAL_EID_ADDRESS = {-64, -88, 2, 0};
    private static final byte[] REMOTE_EID_ADDRESS = {-64, -88, 3, 0};
    private static final byte[] PAIR_2_LOCAL_ADDRESS = {-64, -88, 5, 1};
    private static final byte[] PAIR_1_LOCAL_ADDRESS = {-64, -88, 4, 1};
    private static final byte[] PAIR_2_REMOTE_ADDRESS = {-64, -88, 5, 2};
    private static final byte[] PAIR_1_REMOTE_ADDRESS = {-64, -88, 4, 2};

    private static final GpeLocator[] ABAB_LOCATORS = {
            gpeLocator(PAIR_1_LOCAL_ADDRESS, 1, 3),
            gpeLocator(PAIR_2_LOCAL_ADDRESS, 1, 2),
            gpeLocator(PAIR_1_REMOTE_ADDRESS, 1, 0),
            gpeLocator(PAIR_2_REMOTE_ADDRESS, 1, 0)};

    private static final GpeLocator[] BABA_LOCATORS = {
            ABAB_LOCATORS[1], ABAB_LOCATORS[0], ABAB_LOCATORS[3], ABAB_LOCATORS[2]};

    private static final int LOCAL_EID_PREFIX = 24;
    private static final int REMOTE_EID_PREFIX = 16;
    private static final String GPE_ENTRY_CTX = "gpe-entry-ctx";
    private static final int GPE_FWD_ENTRY_INDEX = 4;

    private NamingContext gpeEntryMappingContext;

    @Captor
    private ArgumentCaptor<GpeAddDelFwdEntry> requestCaptor;

    @Mock
    private GpeStateCheckService gpeStateCheckService;

    private InstanceIdentifier<GpeEntry> id;
    private GpeForwardEntryCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        gpeEntryMappingContext = new NamingContext("gpe-entry-", GPE_ENTRY_CTX);
        id = InstanceIdentifier.create(GpeEntryTable.class)
                .child(GpeEntry.class, new GpeEntryKey(GPE_ENTRY_ID));
        customizer = new GpeForwardEntryCustomizer(api, gpeStateCheckService, gpeEntryMappingContext);
    }

    @SchemaContextProvider
    public ModuleInfoBackedContext schemaContext() {
        return provideSchemaContextFor(ImmutableSet.of($YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.$YangModuleInfoImpl
                        .getInstance(),
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801.$YangModuleInfoImpl
                        .getInstance()));
    }

    @Test
    public void testWriteCurrentAttributesFull(@InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-full.json",
            id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(entryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.writeCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertFullRequest(true, requestCaptor.getValue());
        verify(mappingContext, times(1))
                .put(mappingIid(entry.getId(), GPE_ENTRY_CTX), mapping(entry.getId(), GPE_FWD_ENTRY_INDEX).get());
    }

    private static GpeAddDelFwdEntryReply entryReply() {
        final GpeAddDelFwdEntryReply reply = new GpeAddDelFwdEntryReply();
        reply.fwdEntryIndex = GPE_FWD_ENTRY_INDEX;
        return reply;
    }

    @Test
    public void testWriteCurrentAttributesWithoutLocators(
            @InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-without-locators.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(entryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.writeCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertLocatorLessRequest(true, requestCaptor.getValue());
        verify(mappingContext, times(1))
                .put(mappingIid(entry.getId(), GPE_ENTRY_CTX), mapping(entry.getId(), GPE_FWD_ENTRY_INDEX).get());
    }

    @Test
    public void testWriteCurrentAttributesWithoutAction(
            @InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-without-action.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(entryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.writeCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertActionLessRequest(true, requestCaptor.getValue());
        verify(mappingContext, times(1))
                .put(mappingIid(entry.getId(), GPE_ENTRY_CTX), mapping(entry.getId(), GPE_FWD_ENTRY_INDEX).get());
    }

    /**
     * Gpe entry allows no local eid
     */
    @Test
    public void testWriteCurrentAttributesNoLocalEid(
            @InjectTestData(resourcePath = "/gpe/invalid/invalid-gpe-fwd-entry-no-local-eid.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(entryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.writeCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertActionLessNoLeidRequest(true, requestCaptor.getValue());
        verify(mappingContext, times(1))
                .put(mappingIid(entry.getId(), GPE_ENTRY_CTX), mapping(entry.getId(), GPE_FWD_ENTRY_INDEX).get());
    }

    @Test
    public void testWriteCurrentAttributesFailNoRemoteEid(
            @InjectTestData(resourcePath = "/gpe/invalid/invalid-gpe-fwd-entry-no-remote-eid.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        try {
            customizer.writeCurrentAttributes(id, entryTable.getGpeEntry().get(0), writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have failed");
    }

    @Test
    public void testDeleteCurrentAttributesFull(@InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-full.json",
            id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(new GpeAddDelFwdEntryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.deleteCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertFullRequest(false, requestCaptor.getValue());
        verify(mappingContext, times(1)).delete(mappingIid(entry.getId(), GPE_ENTRY_CTX));
    }

    @Test
    public void testDeleteCurrentAttributesWithoutLocators(
            @InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-without-locators.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(new GpeAddDelFwdEntryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.deleteCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertLocatorLessRequest(false, requestCaptor.getValue());
        verify(mappingContext, times(1)).delete(mappingIid(entry.getId(), GPE_ENTRY_CTX));
    }

    @Test
    public void testDeleteCurrentAttributesWithoutAction(
            @InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-without-action.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(new GpeAddDelFwdEntryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.deleteCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertActionLessRequest(false, requestCaptor.getValue());
        verify(mappingContext, times(1)).delete(mappingIid(entry.getId(), GPE_ENTRY_CTX));
    }

    @Test
    public void testDeleteCurrentAttributesNoLocalEid(
            @InjectTestData(resourcePath = "/gpe/invalid/invalid-gpe-fwd-entry-no-local-eid.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(new GpeAddDelFwdEntryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.deleteCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertActionLessNoLeidRequest(false, requestCaptor.getValue());
        verify(mappingContext, times(1)).delete(mappingIid(entry.getId(), GPE_ENTRY_CTX));
    }

    @Test
    public void testDeleteCurrentAttributesFailNoRemoteEid(
            @InjectTestData(resourcePath = "/gpe/invalid/invalid-gpe-fwd-entry-no-remote-eid.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        try {
            customizer.deleteCurrentAttributes(id, entryTable.getGpeEntry().get(0), writeContext);
        } catch (IllegalArgumentException e) {
            verifyZeroInteractions(api);
            return;
        }
        fail("Test should have failed");
    }

    private void assertActionLessNoLeidRequest(final boolean add, final GpeAddDelFwdEntry actual) {

        assertEquals(booleanToByte(add), actual.isAdd);
        assertEquals(10, actual.dpTable);
        assertEquals(12, actual.vni);
        assertEquals(0, actual.eidType);
        assertEquals(4, actual.locNum);
        assertTrue(Arrays.equals(REMOTE_EID_ADDRESS, actual.rmtEid));
        assertEquals(REMOTE_EID_PREFIX, actual.rmtLen);
        assertTrue(Arrays.equals(ABAB_LOCATORS, actual.locs) || Arrays.equals(BABA_LOCATORS, actual.locs));
    }

    private void assertActionLessRequest(final boolean add, final GpeAddDelFwdEntry actual) {

        assertEquals(booleanToByte(add), actual.isAdd);
        assertEquals(10, actual.dpTable);
        assertEquals(12, actual.vni);
        assertEquals(0, actual.eidType);
        assertEquals(0, actual.action);
        assertEquals(0, actual.locNum);
    }

    private void assertLocatorLessRequest(final boolean add, final GpeAddDelFwdEntry actual) {

        assertEquals(booleanToByte(add), actual.isAdd);
        assertEquals(10, actual.dpTable);
        assertEquals(12, actual.vni);
        assertEquals(0, actual.eidType);
        assertEquals(1, actual.action);
        assertEquals(0, actual.locNum);
        assertTrue(Arrays.equals(LOCAL_EID_ADDRESS, actual.lclEid));
        assertEquals(LOCAL_EID_PREFIX, actual.lclLen);
        assertTrue(Arrays.equals(REMOTE_EID_ADDRESS, actual.rmtEid));
        assertEquals(REMOTE_EID_PREFIX, actual.rmtLen);
    }


    private void assertFullRequest(final boolean add, final GpeAddDelFwdEntry actual) {

        assertEquals(booleanToByte(add), actual.isAdd);
        assertEquals(10, actual.dpTable);
        assertEquals(12, actual.vni);
        assertEquals(0, actual.eidType);
        assertEquals(4, actual.locNum);
        assertTrue(Arrays.equals(LOCAL_EID_ADDRESS, actual.lclEid));
        assertEquals(LOCAL_EID_PREFIX, actual.lclLen);
        assertTrue(Arrays.equals(REMOTE_EID_ADDRESS, actual.rmtEid));
        assertEquals(REMOTE_EID_PREFIX, actual.rmtLen);
        assertTrue(Arrays.equals(ABAB_LOCATORS, actual.locs) || Arrays.equals(BABA_LOCATORS, actual.locs));
    }

    private static GpeLocator gpeLocator(final byte[] address, final int isIpv4, final int weight) {
        GpeLocator locator = new GpeLocator();
        locator.isIp4 = (byte) isIpv4;
        locator.weight = (byte) weight;
        locator.addr = address;

        return locator;
    }
}