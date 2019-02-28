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

package io.fd.hc2vpp.vppnsh.impl.oper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vppnsh.impl.util.FutureJVppNshCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.jvpp.nsh.dto.NshEntryDetails;
import io.fd.jvpp.nsh.dto.NshEntryDetailsReplyDump;
import io.fd.jvpp.nsh.dto.NshEntryDump;
import io.fd.jvpp.nsh.future.FutureJVppNsh;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.Ethernet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.Ipv4;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.MdType1;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.MdType2;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.NshMdType1StateAugment;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.NshMdType1StateAugmentBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.NshMdType2StateAugment;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.NshMdType2StateAugmentBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.VppNsh;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.nsh.md.type2.attributes.Md2Data;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.nsh.md.type2.attributes.Md2DataBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.state.NshEntriesBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.state.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.state.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.state.nsh.entries.NshEntryKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader customizer responsible for nsh entry read.<br> to VPP.
 */
public class NshEntryReaderCustomizer extends FutureJVppNshCustomizer
        implements InitializingListReaderCustomizer<NshEntry, NshEntryKey, NshEntryBuilder>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NshEntryReaderCustomizer.class);
    private final NamingContext nshEntryContext;

    public NshEntryReaderCustomizer(@Nonnull final FutureJVppNsh futureJVppNsh,
                                    @Nonnull final NamingContext nshEntryContext) {
        super(futureJVppNsh);
        this.nshEntryContext = checkNotNull(nshEntryContext, "nshEntryContext should not be null");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder,
                      @Nonnull final List<NshEntry> readData) {
        ((NshEntriesBuilder) builder).setNshEntry(readData);
    }

    @Nonnull
    @Override
    public NshEntryBuilder getBuilder(@Nonnull final InstanceIdentifier<NshEntry> id) {
        return new NshEntryBuilder();
    }

    private void setNshEntryMdType1Augment(@Nonnull final NshEntryBuilder builder,
                                           @Nonnull NshEntryDetails nshEntryDetails) {
        final NshMdType1StateAugmentBuilder augmentBuilder = new NshMdType1StateAugmentBuilder();
        augmentBuilder.setC1((long) nshEntryDetails.c1);
        augmentBuilder.setC2((long) nshEntryDetails.c2);
        augmentBuilder.setC3((long) nshEntryDetails.c3);
        augmentBuilder.setC4((long) nshEntryDetails.c4);

        builder.addAugmentation(NshMdType1StateAugment.class, augmentBuilder.build());
    }

    private void setNshEntryMdType2Augment(@Nonnull final NshEntryBuilder builder,
                                           @Nonnull NshEntryDetails nshEntryDetails) {
        final NshMdType2StateAugmentBuilder augmentBuilder = new NshMdType2StateAugmentBuilder();
        final byte md2_len = (nshEntryDetails.tlvLength);
        byte cur_len = 0;
        byte option_len;

        LOG.debug("rd: md2_len={}", md2_len);
        List<Md2Data> md2Datas = new ArrayList<>();
        while(cur_len < md2_len ) {
            Md2DataBuilder md2DataBuilder = new Md2DataBuilder();
            long md2_class = (long)(nshEntryDetails.tlv[cur_len] & 0xFF)
                              + (nshEntryDetails.tlv[cur_len+1] & 0xFF);
            md2DataBuilder.setMd2Class(md2_class);
            md2DataBuilder.setType((short)nshEntryDetails.tlv[cur_len+2]);
            md2DataBuilder.setLen((short)nshEntryDetails.tlv[cur_len+3]);
            option_len = nshEntryDetails.tlv[cur_len+3];
            LOG.debug("rd: option_len={}", option_len);
            byte[] opt_data = new byte[option_len];
            System.arraycopy(nshEntryDetails.tlv, (cur_len+4), opt_data, 0, option_len);
            md2DataBuilder.setMetadata(Arrays.toString(opt_data));
            LOG.debug("rd: Arrays.toString(opt_data)={}", Arrays.toString(opt_data));
            md2Datas.add(md2DataBuilder.build());

            cur_len += (option_len + 4);
        }
        augmentBuilder.setMd2Data(md2Datas);

        builder.addAugmentation(NshMdType2StateAugment.class, augmentBuilder.build());
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<NshEntry> id,
                                      @Nonnull final NshEntryBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading attributes for nsh entry: {}", id);
        final NshEntryKey key = id.firstKeyOf(NshEntry.class);
        checkArgument(key != null, "could not find NshEntry key in {}", id);
        final NshEntryDump request = new NshEntryDump();

        final String entryName = key.getName();
        if (!nshEntryContext.containsIndex(entryName, ctx.getMappingContext())) {
            LOG.debug("Could not find nsh entry {} in the naming context", entryName);
            return;
        }
        request.entryIndex = nshEntryContext.getIndex(entryName, ctx.getMappingContext());

        final CompletionStage<NshEntryDetailsReplyDump> nshEntryDetailsReplyDumpCompletionStage =
                getFutureJVppNsh().nshEntryDump(request);
        final NshEntryDetailsReplyDump reply =
                getReplyForRead(nshEntryDetailsReplyDumpCompletionStage.toCompletableFuture(), id);
        if (reply == null || reply.nshEntryDetails == null || reply.nshEntryDetails.isEmpty()) {
            LOG.debug("Has no Nsh Entry {} in VPP. ", key.getName());
            return;
        }

        LOG.trace("Nsh Entry : {} attributes returned from VPP: {}", key.getName(), reply);

        final NshEntryDetails nshEntryDetails = reply.nshEntryDetails.get(0);
        builder.setName(entryName);
        builder.withKey(key);
        builder.setVersion((short) nshEntryDetails.verOC);
        builder.setLength((short) nshEntryDetails.length);

        switch (nshEntryDetails.nextProtocol) {
            case 1:
                builder.setNextProtocol(Ipv4.class);
                break;
            case 2:
                builder.setNextProtocol(Ipv6.class);
                break;
            case 3:
                builder.setNextProtocol(Ethernet.class);
                break;
            default:
                LOG.trace("Unsupported next protocol for nsh entry: {}", nshEntryDetails.nextProtocol);
                return;
        }

        switch (nshEntryDetails.mdType) {
            case 1: {
                builder.setMdType(MdType1.class);
                setNshEntryMdType1Augment(builder, nshEntryDetails);
                break;
            }
            case 2: {
                builder.setMdType(MdType2.class);
                setNshEntryMdType2Augment(builder, nshEntryDetails);
                break;
            }
            default:
                LOG.trace("Unsupported Mdtype for nsh entry: {}", nshEntryDetails.mdType);
                return;
        }

        builder.setNsp((long) ((nshEntryDetails.nspNsi >> 8) & 0xFFFFFF));
        builder.setNsi((short) (nshEntryDetails.nspNsi & 0xFF));

        if (LOG.isTraceEnabled()) {
            LOG.trace("Attributes for nsh entry {} successfully read: {}", id, builder.build());
        }
    }

    @Nonnull
    @Override
    public List<NshEntryKey> getAllIds(@Nonnull final InstanceIdentifier<NshEntry> id,
                                       @Nonnull final ReadContext context) throws ReadFailedException {
        LOG.debug("Reading list of keys for nsh entry: {}", id);

        final NshEntryDump request = new NshEntryDump();
        request.entryIndex = -1; // dump call

        NshEntryDetailsReplyDump reply;
        try {
            reply = getFutureJVppNsh().nshEntryDump(request).toCompletableFuture().get();
        } catch (Exception e) {
            throw new IllegalStateException("Nsh Entry dump failed", e);
        }

        if (reply == null || reply.nshEntryDetails == null) {
            return Collections.emptyList();
        }

        final int nIdsLength = reply.nshEntryDetails.size();
        LOG.debug("vppstate.NshEntryCustomizer.getAllIds: nIds.length={}", nIdsLength);
        if (nIdsLength == 0) {
            return Collections.emptyList();
        }

        final List<NshEntryKey> allIds = new ArrayList<>(nIdsLength);
        for (NshEntryDetails detail : reply.nshEntryDetails) {
            final String nshName = nshEntryContext.getName(detail.entryIndex, context.getMappingContext());
            LOG.debug("vppstate.NshEntryCustomizer.getAllIds: nName={}", nshName);
            allIds.add(new NshEntryKey(nshName));
        }

        return allIds;
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.nsh.entries.NshEntry> init(
            @Nonnull final InstanceIdentifier<NshEntry> id, @Nonnull final NshEntry readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(
                InstanceIdentifier.create(VppNsh.class).child(
                        org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.NshEntries.class).child(
                        org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.nsh.entries.NshEntry.class,
                        new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.nsh.entries.NshEntryKey(id.firstKeyOf(NshEntry.class).getName())),
                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.nsh.rev170315.vpp.nsh.nsh.entries.NshEntryBuilder(readValue)
                        .setName(readValue.getName())
                        .build());
    }
}
