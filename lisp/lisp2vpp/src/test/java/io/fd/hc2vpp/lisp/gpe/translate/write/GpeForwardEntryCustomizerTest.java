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
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPair;
import io.fd.hc2vpp.lisp.gpe.translate.ctx.GpeLocatorPairMappingContext;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckService;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.vpp.jvpp.core.dto.GpeAddDelFwdEntry;
import io.fd.vpp.jvpp.core.dto.GpeAddDelFwdEntryReply;
import io.fd.vpp.jvpp.core.types.GpeLocator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.GpeEntryTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.GpeEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocatorPairs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.$YangModuleInfoImpl;
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
    private static final int LOCAL_EID_PREFIX = 24;
    private static final int REMOTE_EID_PREFIX = 16;
    public static final String GPE_ENTRY_CTX = "gpe-entry-ctx";
    public static final int GPE_FWD_ENTRY_INDEX = 4;

    private NamingContext gpeEntryMappingContext;

    @Captor
    private ArgumentCaptor<GpeAddDelFwdEntry> requestCaptor;

    @Mock
    private GpeLocatorPairMappingContext gpeLocatorPairMappingContext;

    @Mock
    private GpeStateCheckService gpeStateCheckService;

    private InstanceIdentifier<GpeEntry> id;
    private GpeForwardEntryCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        gpeEntryMappingContext = new NamingContext("gpe-entry-", GPE_ENTRY_CTX);
        id = InstanceIdentifier.create(GpeEntryTable.class)
                .child(GpeEntry.class, new GpeEntryKey(GPE_ENTRY_ID));
        customizer = new GpeForwardEntryCustomizer(api, gpeStateCheckService, gpeEntryMappingContext,
                gpeLocatorPairMappingContext);
    }

    @SchemaContextProvider
    public ModuleInfoBackedContext schemaContext() {
        return provideSchemaContextFor(ImmutableSet.of($YangModuleInfoImpl.getInstance(),
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.$YangModuleInfoImpl
                        .getInstance(),
                org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170518.$YangModuleInfoImpl
                        .getInstance()));
    }

    @Test
    public void testWriteCurrentAttributesFull(@InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-full.json",
            id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(entryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.writeCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertEquals(expectedFullRequest(true), requestCaptor.getValue());
        verify(mappingContext, times(1))
                .put(mappingIid(entry.getId(), GPE_ENTRY_CTX), mapping(entry.getId(), GPE_FWD_ENTRY_INDEX).get());

        final LocatorPairs locatorPairFirst = entry.getLocatorPairs().get(0);
        final LocatorPairs locatorPairSecond = entry.getLocatorPairs().get(1);
        verify(gpeLocatorPairMappingContext, times(1))
                .addMapping(entry.getId(), locatorPairFirst.getId(),
                        GpeLocatorPair.fromLocatorPair(locatorPairFirst), mappingContext);
        verify(gpeLocatorPairMappingContext, times(1))
                .addMapping(entry.getId(), locatorPairSecond.getId(),
                        GpeLocatorPair.fromLocatorPair(locatorPairSecond), mappingContext);
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
        assertEquals(expectedLocatorLessRequest(true), requestCaptor.getValue());
        verify(mappingContext, times(1))
                .put(mappingIid(entry.getId(), GPE_ENTRY_CTX), mapping(entry.getId(), GPE_FWD_ENTRY_INDEX).get());
        verifyZeroInteractions(gpeLocatorPairMappingContext);
    }

    @Test
    public void testWriteCurrentAttributesWithoutAction(
            @InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-without-action.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(entryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.writeCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertEquals(expectedActionLessRequest(true), requestCaptor.getValue());
        verify(mappingContext, times(1))
                .put(mappingIid(entry.getId(), GPE_ENTRY_CTX), mapping(entry.getId(), GPE_FWD_ENTRY_INDEX).get());
        verifyZeroInteractions(gpeLocatorPairMappingContext);
    }

    /**
     * Gpe entry allows no local eid
     * */
    @Test
    public void testWriteCurrentAttributesNoLocalEid(
            @InjectTestData(resourcePath = "/gpe/invalid/invalid-gpe-fwd-entry-no-local-eid.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(entryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.writeCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertEquals(expectedActionLessNoLeidRequest(true), requestCaptor.getValue());
        verify(mappingContext, times(1))
                .put(mappingIid(entry.getId(), GPE_ENTRY_CTX), mapping(entry.getId(), GPE_FWD_ENTRY_INDEX).get());

        final LocatorPairs locatorPairFirst = entry.getLocatorPairs().get(0);
        final LocatorPairs locatorPairSecond = entry.getLocatorPairs().get(1);
        verify(gpeLocatorPairMappingContext, times(1))
                .addMapping(entry.getId(), locatorPairFirst.getId(),
                        GpeLocatorPair.fromLocatorPair(locatorPairFirst), mappingContext);
        verify(gpeLocatorPairMappingContext, times(1))
                .addMapping(entry.getId(), locatorPairSecond.getId(),
                        GpeLocatorPair.fromLocatorPair(locatorPairSecond), mappingContext);
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
        assertEquals(expectedFullRequest(false), requestCaptor.getValue());
        verify(mappingContext, times(1)).delete(mappingIid(entry.getId(), GPE_ENTRY_CTX));
        verify(gpeLocatorPairMappingContext, times(1))
                .removeMapping(entry.getId(), mappingContext);
    }

    @Test
    public void testDeleteCurrentAttributesWithoutLocators(
            @InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-without-locators.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(new GpeAddDelFwdEntryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.deleteCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertEquals(expectedLocatorLessRequest(false), requestCaptor.getValue());
        verify(mappingContext, times(1)).delete(mappingIid(entry.getId(), GPE_ENTRY_CTX));
        verify(gpeLocatorPairMappingContext, times(1))
                .removeMapping(entry.getId(), mappingContext);
    }

    @Test
    public void testDeleteCurrentAttributesWithoutAction(
            @InjectTestData(resourcePath = "/gpe/gpe-fwd-entry-without-action.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(new GpeAddDelFwdEntryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.deleteCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertEquals(expectedActionLessRequest(false), requestCaptor.getValue());
        verify(mappingContext, times(1)).delete(mappingIid(entry.getId(), GPE_ENTRY_CTX));
        verify(gpeLocatorPairMappingContext, times(1))
                .removeMapping(entry.getId(), mappingContext);
    }

    @Test
    public void testDeleteCurrentAttributesNoLocalEid(
            @InjectTestData(resourcePath = "/gpe/invalid/invalid-gpe-fwd-entry-no-local-eid.json",
                    id = GPE_ENTRY_PATH) GpeEntryTable entryTable) throws Exception {
        when(api.gpeAddDelFwdEntry(any())).thenReturn(future(new GpeAddDelFwdEntryReply()));
        final GpeEntry entry = entryTable.getGpeEntry().get(0);
        customizer.deleteCurrentAttributes(id, entry, writeContext);
        verify(api, times(1)).gpeAddDelFwdEntry(requestCaptor.capture());
        assertEquals(expectedActionLessNoLeidRequest(false), requestCaptor.getValue());
        verify(mappingContext, times(1)).delete(mappingIid(entry.getId(), GPE_ENTRY_CTX));
        verify(gpeLocatorPairMappingContext, times(1))
                .removeMapping(entry.getId(), mappingContext);
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

    private GpeAddDelFwdEntry expectedActionLessNoLeidRequest(final boolean add) {
        final GpeAddDelFwdEntry request = new GpeAddDelFwdEntry();

        request.isAdd = booleanToByte(add);
        request.dpTable = 10;
        request.vni = 12;
        request.eidType = 0;
        request.action = 0;
        request.rmtEid = REMOTE_EID_ADDRESS;
        request.rmtLen = REMOTE_EID_PREFIX;
        request.locNum = 4;
        request.locs = new GpeLocator[]{
                gpeLocator(PAIR_1_LOCAL_ADDRESS, 1, 3),
                gpeLocator(PAIR_2_LOCAL_ADDRESS, 1, 2),
                gpeLocator(PAIR_1_REMOTE_ADDRESS, 1, 0),
                gpeLocator(PAIR_2_REMOTE_ADDRESS, 1, 0)
        };
        return request;
    }

    private GpeAddDelFwdEntry expectedActionLessRequest(final boolean add) {
        final GpeAddDelFwdEntry request = new GpeAddDelFwdEntry();

        request.isAdd = booleanToByte(add);
        request.dpTable = 10;
        request.vni = 12;
        request.eidType = 0;
        request.action = 0;
        request.lclEid = LOCAL_EID_ADDRESS;
        request.lclLen = LOCAL_EID_PREFIX;
        request.rmtEid = REMOTE_EID_ADDRESS;
        request.rmtLen = REMOTE_EID_PREFIX;
        request.locNum = 0;
        return request;
    }

    private GpeAddDelFwdEntry expectedLocatorLessRequest(final boolean add) {
        final GpeAddDelFwdEntry request = new GpeAddDelFwdEntry();

        request.isAdd = booleanToByte(add);
        request.dpTable = 10;
        request.vni = 12;
        request.eidType = 0;
        request.action = 1;
        request.lclEid = LOCAL_EID_ADDRESS;
        request.lclLen = LOCAL_EID_PREFIX;
        request.rmtEid = REMOTE_EID_ADDRESS;
        request.rmtLen = REMOTE_EID_PREFIX;
        request.locNum = 0;
        return request;
    }


    private GpeAddDelFwdEntry expectedFullRequest(final boolean add) {
        final GpeAddDelFwdEntry request = new GpeAddDelFwdEntry();

        request.isAdd = booleanToByte(add);
        request.dpTable = 10;
        request.vni = 12;
        request.eidType = 0;
        request.action = 1;
        request.lclEid = LOCAL_EID_ADDRESS;
        request.lclLen = LOCAL_EID_PREFIX;
        request.rmtEid = REMOTE_EID_ADDRESS;
        request.rmtLen = REMOTE_EID_PREFIX;
        request.locNum = 4;
        request.locs = new GpeLocator[]{
                gpeLocator(PAIR_1_LOCAL_ADDRESS, 1, 3),
                gpeLocator(PAIR_2_LOCAL_ADDRESS, 1, 2),
                gpeLocator(PAIR_1_REMOTE_ADDRESS, 1, 0),
                gpeLocator(PAIR_2_REMOTE_ADDRESS, 1, 0)
        };

        return request;
    }

    private GpeLocator gpeLocator(final byte[] address, final int isIpv4, final int weight) {
        GpeLocator locator = new GpeLocator();
        locator.isIp4 = (byte) isIpv4;
        locator.weight = (byte) weight;
        locator.addr = address;

        return locator;
    }
}