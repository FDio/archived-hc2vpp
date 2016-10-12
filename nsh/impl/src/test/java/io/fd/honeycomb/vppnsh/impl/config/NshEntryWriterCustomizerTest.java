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

package io.fd.honeycomb.vppnsh.impl.config;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.MdType1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1AugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntryKey;

import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.nsh.dto.NshAddDelEntry;
import io.fd.vpp.jvpp.nsh.dto.NshAddDelEntryReply;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;

public class NshEntryWriterCustomizerTest extends WriterCustomizerTest {

    private static final String ENTRY_CTX_NAME = "nsh-entry-instance";
    private static final int ENTRY_INDEX = 1;
    private static final String ENTRY_NAME = "entry1";

    @Mock
    protected FutureJVppNsh jvppNsh;

    private NamingContext nshContext;

    private NshEntryWriterCustomizer customizer;

    @Override
    public void setUp() throws Exception {
        nshContext = new NamingContext("nsh_entry", ENTRY_CTX_NAME);
        defineMapping(mappingContext, ENTRY_NAME, ENTRY_INDEX, ENTRY_CTX_NAME);

        customizer = new NshEntryWriterCustomizer(jvppNsh, nshContext);
    }

    private static NshEntry generateNshEntry(final String name) {
        final NshEntryBuilder builder = new NshEntryBuilder();
        builder.setName(name);
        builder.setKey(new NshEntryKey(name));
        builder.setVersion((short) 0);
        builder.setLength((short) 6);
        builder.setMdType(MdType1.class);
        builder.setNextProtocol(Ethernet.class);
        builder.setNsp(123L);
        builder.setNsi((short) 4);

        final NshMdType1AugmentBuilder augmentBuilder = new NshMdType1AugmentBuilder();
        augmentBuilder.setC1((long) 1);
        augmentBuilder.setC2((long) 2);
        augmentBuilder.setC3((long) 3);
        augmentBuilder.setC4((long) 4);
        builder.addAugmentation(NshMdType1Augment.class, augmentBuilder.build());

        return builder.build();
    }

    private static InstanceIdentifier<NshEntry> getNshEntryId(final String name) {
        return InstanceIdentifier.create(NshEntries.class)
                .child(NshEntry.class, new NshEntryKey(name));
    }

    private void whenNshAddDelEntryThenSuccess() {
        final NshAddDelEntryReply reply = new NshAddDelEntryReply();
        reply.entryIndex = ENTRY_INDEX;
        doReturn(future(reply)).when(jvppNsh).nshAddDelEntry(any(NshAddDelEntry.class));
    }

    private void whenNshAddDelEntryThenFailure() {
        doReturn(failedFuture()).when(jvppNsh).nshAddDelEntry(any(NshAddDelEntry.class));
    }

    private static NshAddDelEntry generateNshAddDelEntry(final byte isAdd) {
        final NshAddDelEntry request = new NshAddDelEntry();
        request.isAdd = isAdd;
        request.verOC = 0;
        request.length = 6;
        request.mdType = 1;
        request.nextProtocol = 3;
        request.nspNsi = 123<<8 | 4;
        request.c1 = 1;
        request.c2 = 2;
        request.c3 = 3;
        request.c4 = 4;

        return request;
    }

    @Test
    public void testCreate() throws Exception {
        final NshEntry nshEntry = generateNshEntry(ENTRY_NAME);
        final InstanceIdentifier<NshEntry> id = getNshEntryId(ENTRY_NAME);

        whenNshAddDelEntryThenSuccess();

        customizer.writeCurrentAttributes(id, nshEntry, writeContext);

        verify(jvppNsh).nshAddDelEntry(generateNshAddDelEntry((byte) 1));

    }

    @Test
    public void testCreateFailed() throws Exception {
        final NshEntry nshEntry = generateNshEntry(ENTRY_NAME);
        final InstanceIdentifier<NshEntry> id = getNshEntryId(ENTRY_NAME);

        whenNshAddDelEntryThenFailure();

        try {
            customizer.writeCurrentAttributes(id, nshEntry, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(jvppNsh).nshAddDelEntry(generateNshAddDelEntry((byte) 1));

            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testDelete() throws Exception {
        final NshEntry nshEntry = generateNshEntry(ENTRY_NAME);
        final InstanceIdentifier<NshEntry> id = getNshEntryId(ENTRY_NAME);

        whenNshAddDelEntryThenSuccess();

        customizer.deleteCurrentAttributes(id, nshEntry, writeContext);

        verify(jvppNsh).nshAddDelEntry(generateNshAddDelEntry((byte) 0));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final NshEntry nshEntry = generateNshEntry(ENTRY_NAME);
        final InstanceIdentifier<NshEntry> id = getNshEntryId(ENTRY_NAME);

        whenNshAddDelEntryThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, nshEntry, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(jvppNsh).nshAddDelEntry(generateNshAddDelEntry((byte) 0));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");

        customizer.deleteCurrentAttributes(id, nshEntry, writeContext);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws Exception {
        final NshEntry nshEntryBefore = generateNshEntry(ENTRY_NAME);
        final InstanceIdentifier<NshEntry> id = getNshEntryId(ENTRY_NAME);
        customizer.updateCurrentAttributes(id, nshEntryBefore, new NshEntryBuilder().build(), writeContext);
    }
}