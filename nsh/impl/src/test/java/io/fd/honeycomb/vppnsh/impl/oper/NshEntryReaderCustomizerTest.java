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

package io.fd.honeycomb.vppnsh.impl.oper;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;

import io.fd.honeycomb.translate.vpp.util.NamingContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.MdType1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1StateAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1StateAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntryKey;

import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.nsh.dto.NshEntryDetails;
import io.fd.vpp.jvpp.nsh.dto.NshEntryDetailsReplyDump;
import io.fd.vpp.jvpp.nsh.dto.NshEntryDump;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;


public class NshEntryReaderCustomizerTest extends
    ListReaderCustomizerTest<NshEntry, NshEntryKey, NshEntryBuilder> {

    private static final String ENTRY_CTX_NAME = "nsh-entry-instance";
    private static final int ENTRY_INDEX_1 = 1;
    private static final String ENTRY_NAME_1 = "entry1";
    private static final int ENTRY_INDEX_2 = 2;
    private static final String ENTRY_NAME_2 = "entry2";

    @Mock
    protected FutureJVppNsh jvppNsh;

    private NamingContext nshContext;

    public NshEntryReaderCustomizerTest() {
        super(NshEntry.class, NshEntriesBuilder.class);
    }

    @Override
    protected ReaderCustomizer<NshEntry, NshEntryBuilder> initCustomizer() {
        return new NshEntryReaderCustomizer(jvppNsh, nshContext);
    }

    private static InstanceIdentifier<NshEntry> getNshEntryId(final String name) {
        return InstanceIdentifier.create(NshEntries.class)
            .child(NshEntry.class, new NshEntryKey(name));
    }

    @Override
    public void setUp() throws VppBaseCallException {
        nshContext = new NamingContext("nsh_entry", ENTRY_CTX_NAME);
        defineMapping(mappingContext, ENTRY_NAME_1, ENTRY_INDEX_1, ENTRY_CTX_NAME);
        defineMapping(mappingContext, ENTRY_NAME_2, ENTRY_INDEX_2, ENTRY_CTX_NAME);

        final NshEntryDetailsReplyDump reply = new NshEntryDetailsReplyDump();
        final NshEntryDetails nshEntryDetails = new NshEntryDetails();
        nshEntryDetails.verOC = 0;
        nshEntryDetails.length = 6;
        nshEntryDetails.mdType = 1;
        nshEntryDetails.nextProtocol = 3;
        nshEntryDetails.nspNsi = (123<<8 | 4);
        nshEntryDetails.c1 = 1;
        nshEntryDetails.c2 = 2;
        nshEntryDetails.c3 = 3;
        nshEntryDetails.c4 = 4;
        reply.nshEntryDetails = Lists.newArrayList(nshEntryDetails);
        doReturn(future(reply)).when(jvppNsh).nshEntryDump(any(NshEntryDump.class));
    }

    @Test
    public void testreadCurrentAttributes() throws ReadFailedException {

        NshEntryBuilder builder = new NshEntryBuilder();
        getCustomizer().readCurrentAttributes(getNshEntryId(ENTRY_NAME_1), builder, ctx);

        assertEquals(0, builder.getVersion().intValue());
        assertEquals(6, builder.getLength().intValue());
        assertEquals(MdType1.class, builder.getMdType());
        assertEquals(Ethernet.class, builder.getNextProtocol());
        assertEquals(123, builder.getNsp().intValue());
        assertEquals(4, builder.getNsi().intValue());
        assertEquals(1, builder.getAugmentation(NshMdType1StateAugment.class).getC1().intValue());
        assertEquals(2, builder.getAugmentation(NshMdType1StateAugment.class).getC2().intValue());
        assertEquals(3, builder.getAugmentation(NshMdType1StateAugment.class).getC3().intValue());
        assertEquals(4, builder.getAugmentation(NshMdType1StateAugment.class).getC4().intValue());

        verify(jvppNsh).nshEntryDump(any(NshEntryDump.class));
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        final NshEntryDetailsReplyDump reply = new NshEntryDetailsReplyDump();

        final NshEntryDetails nshEntryDetails_1 = new NshEntryDetails();
        nshEntryDetails_1.entryIndex = ENTRY_INDEX_1;
        nshEntryDetails_1.verOC = 0;
        nshEntryDetails_1.length = 6;
        nshEntryDetails_1.mdType = 1;
        nshEntryDetails_1.nextProtocol = 3;
        nshEntryDetails_1.nspNsi = (123<<8 | 4);
        nshEntryDetails_1.c1 = 1;
        nshEntryDetails_1.c2 = 2;
        nshEntryDetails_1.c3 = 3;
        nshEntryDetails_1.c4 = 4;
        reply.nshEntryDetails = Lists.newArrayList(nshEntryDetails_1);

        final NshEntryDetails nshEntryDetails_2 = new NshEntryDetails();
        nshEntryDetails_2.entryIndex = ENTRY_INDEX_2;
        nshEntryDetails_2.verOC = 0;
        nshEntryDetails_2.length = 6;
        nshEntryDetails_2.mdType = 1;
        nshEntryDetails_2.nextProtocol = 2;
        nshEntryDetails_2.nspNsi = (223<<8 | 24);
        nshEntryDetails_2.c1 = 21;
        nshEntryDetails_2.c2 = 22;
        nshEntryDetails_2.c3 = 23;
        nshEntryDetails_2.c4 = 24;
        reply.nshEntryDetails = Lists.newArrayList(nshEntryDetails_2);

        doReturn(future(reply)).when(jvppNsh).nshEntryDump(any(NshEntryDump.class));

        final List<NshEntryKey> allIds = getCustomizer().getAllIds(getNshEntryId(ENTRY_NAME_1), ctx);

        assertEquals(reply.nshEntryDetails.size(), allIds.size());

    }
}
