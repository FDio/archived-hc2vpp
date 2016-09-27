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

package io.fd.honeycomb.vppnsh.impl.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.vpp.util.WriteTimeoutException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vppnsh.impl.util.FutureJVppNshCustomizer;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.Ipv6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.MdType1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.nsh.dto.NshAddDelEntry;
import org.openvpp.jvpp.nsh.dto.NshAddDelEntryReply;
import org.openvpp.jvpp.nsh.future.FutureJVppNsh;
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
        try {
            final int newEntryIndex =
                    nshAddDelEntry(true, id, dataAfter, ~0 /* value not present */, writeContext.getMappingContext());

            // Add nsh entry name <-> vpp index mapping to the naming context:
            nshEntryContext.addName(newEntryIndex, dataAfter.getName(), writeContext.getMappingContext());
            LOG.debug("Successfully created nsh entry(id={]): iid={} dataAfter={}", newEntryIndex, id, dataAfter);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<NshEntry> id,
                                        @Nonnull final NshEntry dataBefore, @Nonnull final NshEntry dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Nsh entry update is not supported");
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
        try {
            nshAddDelEntry(false, id, dataBefore, entryIndex, writeContext.getMappingContext());

            // Remove deleted interface from interface context:
            nshEntryContext.removeName(dataBefore.getName(), writeContext.getMappingContext());
            LOG.debug("Successfully removed nsh entry(id={]): iid={} dataAfter={}", entryIndex, id, dataBefore);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private int nshAddDelEntry(final boolean isAdd, @Nonnull final InstanceIdentifier<NshEntry> id,
                               @Nonnull final NshEntry entry, final int entryId, final MappingContext ctx)
            throws VppBaseCallException, WriteTimeoutException {
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
        } else if (entry.getMdType() == MdType1.class) {
            request.mdType = 2;
        } else {
            request.mdType = 0;
        }

        request.nspNsi = (entry.getNsp().intValue() << 8) | entry.getNsi();

        return request;
    }
}
