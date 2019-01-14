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

package io.fd.hc2vpp.v3po.l2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.L2FibAddDel;
import io.fd.vpp.jvpp.core.dto.L2FibAddDelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.L2FibFilter;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev181008.l2.fib.attributes.l2.fib.table.L2FibEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for L2 FIB create/delete operations.<br> Sends {@code l2_fib_add_del} message to
 * VPP.<br> Equivalent of invoking {@code vppctl l2fib add/del} command.
 */
public class L2FibEntryCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<L2FibEntry, L2FibEntryKey>, ByteDataTranslator, MacTranslator,
        JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(L2FibEntryCustomizer.class);
    public static final int NO_INTERFACE_REF = -1;

    private final NamingContext bdContext;
    private final NamingContext interfaceContext;

    public L2FibEntryCustomizer(@Nonnull final FutureJVppCore futureJVppCore, @Nonnull final NamingContext bdContext,
                                @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.bdContext = checkNotNull(bdContext, "bdContext should not be null");
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<L2FibEntry> id,
                                       @Nonnull final L2FibEntry dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        LOG.debug("Creating L2 FIB entry: {} {}", id, dataAfter);
        l2FibAddDel(id, dataAfter, writeContext, true);
        LOG.debug("L2 FIB entry created successfully: {} {}", id, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<L2FibEntry> id,
                                        @Nonnull final L2FibEntry dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        LOG.debug("Deleting L2 FIB entry: {} {}", id, dataBefore);
        l2FibAddDel(id, dataBefore, writeContext, false);
        LOG.debug("L2 FIB entry deleted successfully: {} {}", id, dataBefore);
    }

    private void l2FibAddDel(@Nonnull final InstanceIdentifier<L2FibEntry> id, @Nonnull final L2FibEntry entry,
                             final WriteContext writeContext, boolean isAdd) throws WriteFailedException {
        final String bdName = id.firstKeyOf(BridgeDomain.class).getName();
        final MappingContext mappingContext = writeContext.getMappingContext();
        final int bdId = bdContext.getIndex(bdName, mappingContext);

        final L2FibAddDel l2FibRequest = createL2FibRequest(entry, bdId, getCheckedInterfaceIndex(entry,
                mappingContext), isAdd);
        LOG.debug("Sending l2FibAddDel request: {}", l2FibRequest);
        final CompletionStage<L2FibAddDelReply> l2FibAddDelReplyCompletionStage =
                getFutureJVpp().l2FibAddDel(l2FibRequest);

        getReplyForWrite(l2FibAddDelReplyCompletionStage.toCompletableFuture(), id);
    }

    private int getCheckedInterfaceIndex(final L2FibEntry entry, final MappingContext mappingContext) {
        if (L2FibFilter.class == entry.getAction()) {
            // if filter, interface should not be defined
            checkArgument(entry.getOutgoingInterface() == null, "Interface reference should not be defined for type %s",
                    L2FibFilter.class);
            return NO_INTERFACE_REF;
        } else {
            // if type is not filter, interface reference is mandatory
            return interfaceContext.getIndex(
                    checkNotNull(entry.getOutgoingInterface(), "Interface reference should be defined for type %s",
                            entry.getAction()), mappingContext);
        }
    }

    private L2FibAddDel createL2FibRequest(final L2FibEntry entry, final int bdId, final int swIfIndex, boolean isAdd) {
        final L2FibAddDel request = new L2FibAddDel();
        request.mac = parseMac(entry.getPhysAddress().getValue());
        request.bdId = bdId;
        request.swIfIndex = swIfIndex;
        request.isAdd = booleanToByte(isAdd);
        if (isAdd) {
            request.staticMac = booleanToByte(entry.isStaticConfig());
            request.filterMac = booleanToByte(L2FibFilter.class == entry.getAction());
        }
        return request;
    }
}
