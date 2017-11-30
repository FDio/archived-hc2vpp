/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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

package io.fd.hc2vpp.vppnsh.impl.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vppnsh.impl.util.FutureJVppNshCustomizer;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.nsh.dto.NshAddDelEntry;
import io.fd.vpp.jvpp.nsh.dto.NshAddDelEntryReply;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.MdType1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.MdType2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.NshMdType1Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.NshMdType2Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.nsh.md.type2.attributes.Md2Data;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.vpp.nsh.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev170315.vpp.nsh.nsh.entries.NshEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer customizer responsible for NshEntry create/delete.
 */
public class NshEntryWriterCustomizer extends FutureJVppNshCustomizer
        implements ListWriterCustomizer<NshEntry, NshEntryKey>, ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NshEntryWriterCustomizer.class);
    private final NamingContext nshEntryContext;

    public NshEntryWriterCustomizer(@Nonnull final FutureJVppNsh futureJVppNsh,
                                    @Nonnull final NamingContext nshEntryContext) {
        super(futureJVppNsh);
        this.nshEntryContext = checkNotNull(nshEntryContext, "nshEntryContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<NshEntry> id,
                                       @Nonnull final NshEntry dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Creating nsh entry: iid={} dataAfter={}", id, dataAfter);
        final int newEntryIndex =
                nshAddDelEntry(true, id, dataAfter, ~0 /* value not present */, writeContext.getMappingContext());
        // Add nsh entry name <-> vpp index mapping to the naming context:
        nshEntryContext.addName(newEntryIndex, dataAfter.getName(), writeContext.getMappingContext());
        LOG.debug("Successfully created nsh entry(id={]): iid={} dataAfter={}", newEntryIndex, id, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<NshEntry> id,
                                        @Nonnull final NshEntry dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Removing nsh entry: iid={} dataBefore={}", id, dataBefore);
        final String entryName = dataBefore.getName();
        checkState(nshEntryContext.containsIndex(entryName, writeContext.getMappingContext()),
                "Removing nsh entry {}, but index could not be found in the nsh entry context", entryName);

        final int entryIndex = nshEntryContext.getIndex(entryName, writeContext.getMappingContext());
        nshAddDelEntry(false, id, dataBefore, entryIndex, writeContext.getMappingContext());

        // Remove deleted interface from interface context:
        nshEntryContext.removeName(dataBefore.getName(), writeContext.getMappingContext());
        LOG.debug("Successfully removed nsh entry(id={]): iid={} dataAfter={}", entryIndex, id, dataBefore);
    }

    private int nshAddDelEntry(final boolean isAdd, @Nonnull final InstanceIdentifier<NshEntry> id,
                               @Nonnull final NshEntry entry, final int entryId, final MappingContext ctx)
            throws WriteFailedException {
        final CompletionStage<NshAddDelEntryReply> createNshEntryReplyCompletionStage =
                getFutureJVppNsh().nshAddDelEntry(getNshAddDelEntryRequest(isAdd, entryId, entry, ctx));

        final NshAddDelEntryReply reply =
                getReplyForWrite(createNshEntryReplyCompletionStage.toCompletableFuture(), id);
        return reply.entryIndex;

    }

    private void getNshEntryMdType1Request(@Nonnull final NshEntry entry,
                                           @Nonnull NshAddDelEntry request) {
        final NshMdType1Augment nshMdType1Augment = entry.getAugmentation(NshMdType1Augment.class);
        if (nshMdType1Augment != null) {
            request.c1 = (int) nshMdType1Augment.getC1().longValue();
            request.c2 = (int) nshMdType1Augment.getC2().longValue();
            request.c3 = (int) nshMdType1Augment.getC3().longValue();
            request.c4 = (int) nshMdType1Augment.getC4().longValue();
        }
    }

    private void getNshEntryMdType2Request(@Nonnull final NshEntry entry, @Nonnull NshAddDelEntry request) {
        final List<Md2Data> md2Datas = entry.getAugmentation(NshMdType2Augment.class).getMd2Data();
        final byte md2_len = (byte) (entry.getLength() * 4 - 8);
        byte cur_len = 0;
        byte option_len;

        LOG.debug("wr: md2_len={}", md2_len);
        request.tlv = new byte[md2_len];
        for (Md2Data md2data : md2Datas) {
            option_len = (byte) md2data.getLen().shortValue();
            LOG.debug("wr: option_len={}", option_len);
            if ((cur_len + option_len + 4) <= md2_len) {
                request.tlv[cur_len] = (byte) (md2data.getMd2Class().shortValue() >> 8);
                request.tlv[cur_len + 1] = (byte) (md2data.getMd2Class().shortValue() & 0xF);
                request.tlv[cur_len + 2] = (byte) md2data.getType().shortValue();
                request.tlv[cur_len + 3] = option_len;

                /* convert string to hex digits */
                LOG.debug("wr: md2data.getMetadata()={}", md2data.getMetadata());
                int length = md2data.getMetadata().length();
                for (int i = 0; i < length / 2; ++i)
                {
                    request.tlv[(cur_len+4)+i] = (byte)(Integer.parseInt
                            (md2data.getMetadata().substring(i*2, i*2+2), 16) & 0xff);
                }
                cur_len += (option_len + 4);
            }
        }
        request.tlvLength = cur_len;
    }

    private NshAddDelEntry getNshAddDelEntryRequest(final boolean isAdd, final int entryIndex,
                                                    @Nonnull final NshEntry entry,
                                                    @Nonnull final MappingContext ctx) {
        final NshAddDelEntry request = new NshAddDelEntry();
        request.isAdd = booleanToByte(isAdd);

        request.verOC = (byte) entry.getVersion().shortValue();
        request.length = (byte) entry.getLength().intValue();
        if (entry.getNextProtocol() == Ipv4.class) {
            request.nextProtocol = 1;
        } else if (entry.getNextProtocol() == Ipv6.class) {
            request.nextProtocol = 2;
        } else if (entry.getNextProtocol() == Ethernet.class) {
            request.nextProtocol = 3;
        } else {
            request.nextProtocol = 0;
        }

        if (entry.getMdType() == MdType1.class) {
            request.mdType = 1;
            getNshEntryMdType1Request(entry, request);
        } else if (entry.getMdType() == MdType2.class) {
            request.mdType = 2;
            getNshEntryMdType2Request(entry, request);
        } else {
            request.mdType = 0;
        }

        request.nspNsi = (entry.getNsp().intValue() << 8) | entry.getNsi();

        return request;
    }
}
