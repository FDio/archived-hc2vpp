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

package io.fd.honeycomb.v3po.translate.v3po.vpp;

import static io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils.booleanToByte;
import static io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils.parseMac;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.L2FibFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.L2FibAddDel;
import org.openvpp.jvpp.dto.L2FibAddDelReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for L2 FIB create/delete operations.<br> Sends {@code l2_fib_add_del} message to
 * VPP.<br> Equivalent of invoking {@code vppctl l2fib add/del} command.
 */
public class L2FibEntryCustomizer extends FutureJVppCustomizer
    implements ListWriterCustomizer<L2FibEntry, L2FibEntryKey> {

    private static final Logger LOG = LoggerFactory.getLogger(L2FibEntryCustomizer.class);

    private final NamingContext bdContext;
    private final NamingContext interfaceContext;

    public L2FibEntryCustomizer(@Nonnull final FutureJVpp futureJvpp, @Nonnull final NamingContext bdContext,
                                @Nonnull final NamingContext interfaceContext) {
        super(futureJvpp);
        this.bdContext = Preconditions.checkNotNull(bdContext, "bdContext should not be null");
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Nonnull
    @Override
    public Optional<List<L2FibEntry>> extract(@Nonnull final InstanceIdentifier<L2FibEntry> currentId,
                                              @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((L2FibTable) parentData).getL2FibEntry());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<L2FibEntry> id,
                                       @Nonnull final L2FibEntry dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException.CreateFailedException {
        try {
            LOG.debug("Creating L2 FIB entry: {} {}", id, dataAfter);
            l2FibAddDel(id, dataAfter, writeContext, true);
            LOG.debug("L2 FIB entry created successfully: {} {}", id, dataAfter);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to create L2 FIB entry: {} {}", id, dataAfter);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<L2FibEntry> id,
                                        @Nonnull final L2FibEntry dataBefore, @Nonnull final L2FibEntry dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException(
            "L2 FIB entry update is not supported. It has to be deleted and then created.");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<L2FibEntry> id,
                                        @Nonnull final L2FibEntry dataBefore, @Nonnull final WriteContext writeContext)
        throws WriteFailedException.DeleteFailedException {
        try {
            LOG.debug("Deleting L2 FIB entry: {} {}", id, dataBefore);
            l2FibAddDel(id, dataBefore, writeContext, false);
            LOG.debug("L2 FIB entry deleted successfully: {} {}", id, dataBefore);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to delete L2 FIB entry: {} {}", id, dataBefore);
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void l2FibAddDel(@Nonnull final InstanceIdentifier<L2FibEntry> id, @Nonnull final L2FibEntry entry,
                             final WriteContext writeContext, boolean isAdd) throws VppBaseCallException {
        final String bdName = id.firstKeyOf(BridgeDomain.class).getName();
        final int bdId = bdContext.getIndex(bdName, writeContext.getMappingContext());

        int swIfIndex = -1;
        final String swIfName = entry.getOutgoingInterface();
        if (swIfName != null) {
            swIfIndex = interfaceContext.getIndex(swIfName, writeContext.getMappingContext());
        }

        final L2FibAddDel l2FibRequest = createL2FibRequest(entry, bdId, swIfIndex, isAdd);
        LOG.debug("Sending l2FibAddDel request: {}", ReflectionToStringBuilder.toString(l2FibRequest));
        final CompletionStage<L2FibAddDelReply> l2FibAddDelReplyCompletionStage =
            getFutureJVpp().l2FibAddDel(l2FibRequest);

        TranslateUtils.getReply(l2FibAddDelReplyCompletionStage.toCompletableFuture());
    }

    private L2FibAddDel createL2FibRequest(final L2FibEntry entry, final int bdId, final int swIfIndex, boolean isAdd) {
        final L2FibAddDel request = new L2FibAddDel();
        request.mac = macToLong(entry.getPhysAddress().getValue());
        request.bdId = bdId;
        request.swIfIndex = swIfIndex;
        request.isAdd = booleanToByte(isAdd);
        if (isAdd) {
            request.staticMac = booleanToByte(entry.isStaticConfig());
            request.filterMac = booleanToByte(L2FibFilter.class == entry.getAction());
        }
        return request;
    }

    // mac address is string of the form: 11:22:33:44:55:66
    // but VPP expects long value in the format 11:22:33:44:55:66:XX:XX
    private static long macToLong(final String macAddress) {
        final byte[] mac = parseMac(macAddress);
        return Longs.fromBytes(mac[0], mac[1], mac[2], mac[3],
            mac[4], mac[5], (byte) 0, (byte) 0);
    }
}
