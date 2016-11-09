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

package io.fd.honeycomb.lisp.translate.write;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType;

import io.fd.honeycomb.lisp.context.util.AdjacenciesMappingContext;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.LispAddDelAdjacency;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.AdjacencyKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.LocalEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.adjacencies.grouping.adjacencies.adjacency.RemoteEid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AdjacencyCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Adjacency, AdjacencyKey>, ByteDataTranslator, EidTranslator,
        JvppReplyConsumer {

    private final EidMappingContext localEidsMappingContext;
    private final EidMappingContext remoteEidsMappingContext;
    private final AdjacenciesMappingContext adjacenciesMappingContext;

    public AdjacencyCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                               @Nonnull final EidMappingContext localEidsMappingContext,
                               @Nonnull final EidMappingContext remoteEidsMappingContext,
                               @Nonnull final AdjacenciesMappingContext adjacenciesMappingContext) {
        super(futureJvpp);
        this.localEidsMappingContext =
                checkNotNull(localEidsMappingContext, "Eid context for local eid's cannot be null");
        this.remoteEidsMappingContext =
                checkNotNull(remoteEidsMappingContext, "Eid context for remote eid's cannot be null");
        this.adjacenciesMappingContext = checkNotNull(adjacenciesMappingContext, "Adjacencies context cannot be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Adjacency> id,
                                       @Nonnull final Adjacency dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        try {
            addDelAdjacency(true, id, dataAfter, writeContext);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        //after successful creation, create mapping
        adjacenciesMappingContext.addEidPair(adjacencyId(id),
                localEidId(dataAfter, writeContext),
                remoteEidId(dataAfter, writeContext),
                writeContext.getMappingContext());
    }

    private String remoteEidId(final @Nonnull Adjacency dataAfter, final @Nonnull WriteContext writeContext) {
        return remoteEidsMappingContext.getId(toRemoteEid(dataAfter.getRemoteEid()), writeContext.getMappingContext())
                .getValue();
    }

    private String localEidId(final @Nonnull Adjacency dataAfter, final @Nonnull WriteContext writeContext) {
        return localEidsMappingContext.getId(toLocalEid(dataAfter.getLocalEid()), writeContext.getMappingContext())
                .getValue();
    }

    private String adjacencyId(final @Nonnull InstanceIdentifier<Adjacency> id) {
        return id.firstKeyOf(Adjacency.class).getId();
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Adjacency> id,
                                        @Nonnull final Adjacency dataBefore, @Nonnull final Adjacency dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Adjacency> id,
                                        @Nonnull final Adjacency dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        try {
            addDelAdjacency(false, id, dataBefore, writeContext);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }

        //after successful creation, create mapping
        adjacenciesMappingContext.removeForIndex(adjacencyId(id),
                writeContext.getMappingContext());
    }

    private void addDelAdjacency(boolean add, final InstanceIdentifier<Adjacency> id, final Adjacency data,
                                 final WriteContext writeContext)
            throws TimeoutException, VppBaseCallException {

        final int vni = checkNotNull(id.firstKeyOf(VniTable.class), "Unable to find parent VNI for {}", id)
                .getVirtualNetworkIdentifier().intValue();

        // both local and remote eids must be referenced to have respective mapping,
        // if there is an attempt to add adjacency.
        // In our case its enough to check if local/remote mapping exist for respective eid,
        // because such mappings are created while creating mappings
        final LocalEid localEid = add
                ? verifiedLocalEid(data.getLocalEid(), writeContext)
                : data.getLocalEid();
        final RemoteEid remoteEid = add
                ? verifiedRemoteEid(data.getRemoteEid(), writeContext)
                : data.getRemoteEid();
        final EidType localEidType = getEidType(localEid);
        final EidType remoteEidType = getEidType(data.getRemoteEid());

        checkArgument(localEidType ==
                remoteEidType, "Local[%s] and Remote[%s] eid types must be the same", localEidType, remoteEidType);

        LispAddDelAdjacency request = new LispAddDelAdjacency();

        request.isAdd = booleanToByte(add);
        request.leid = getEidAsByteArray(localEid);
        request.leidLen = getPrefixLength(localEid);
        request.reid = getEidAsByteArray(remoteEid);
        request.reidLen = getPrefixLength(remoteEid);
        request.eidType = (byte) localEidType.getValue();
        request.vni = vni;

        getReply(getFutureJVpp().lispAddDelAdjacency(request).toCompletableFuture());
    }

    private LocalEid verifiedLocalEid(final LocalEid localEid, final WriteContext writeContext) {
        if (localEidsMappingContext.containsId(toLocalEid(localEid), writeContext.getMappingContext())) {
            return localEid;
        }
        throw new IllegalStateException(
                "Referenced Local Eid[" + localEid +
                        "] doesn't have local mapping defined, therefore it can't be used in adjacency");
    }

    private RemoteEid verifiedRemoteEid(final RemoteEid remoteEid, final WriteContext writeContext) {
        if (remoteEidsMappingContext.containsId(toRemoteEid(remoteEid), writeContext.getMappingContext())) {
            return remoteEid;
        }
        throw new IllegalStateException(
                "Referenced Remote Eid[" + remoteEid +
                        "] doesn't have remote mapping defined, therefore it can't be used in adjacency");
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.Eid toRemoteEid(
            final RemoteEid remoteEid) {
        return newRemoteEidBuilder(remoteEid.getAddressType(), remoteEid.getVirtualNetworkId().getValue().intValue())
                .setAddress(remoteEid.getAddress()).build();
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.Eid toLocalEid(
            LocalEid localEid) {
        return newLocalEidBuilder(localEid.getAddressType(), localEid.getVirtualNetworkId().getValue().intValue())
                .setAddress(localEid.getAddress()).build();
    }
}
