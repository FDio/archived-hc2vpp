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

package io.fd.honeycomb.vppnsh.impl.oper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vppnsh.impl.util.FutureJVppNshCustomizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.MdType1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1StateAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1StateAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntryKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.nsh.dto.NshEntryDetails;
import org.openvpp.jvpp.nsh.dto.NshEntryDetailsReplyDump;
import org.openvpp.jvpp.nsh.dto.NshEntryDump;
import org.openvpp.jvpp.nsh.future.FutureJVppNsh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader customizer responsible for nsh entry read.<br> to VPP.
 */
public class NshEntryReaderCustomizer extends FutureJVppNshCustomizer
        implements ListReaderCustomizer<NshEntry, NshEntryKey, NshEntryBuilder>, JvppReplyConsumer {

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

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<NshEntry> id,
                                      @Nonnull final NshEntryBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading attributes for nsh entry: {}", id);
        try {
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
            builder.setKey(key);
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
                    builder.setMdType(MdType1.class);
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
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to readCurrentAttributes for: {}", id);
            throw new ReadFailedException(id, e);
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
}
