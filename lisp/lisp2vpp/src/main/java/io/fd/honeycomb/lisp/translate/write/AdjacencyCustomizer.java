/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType;

import io.fd.honeycomb.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.Adjacency;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.adjacencies.AdjacencyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.LispAddDelAdjacency;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class AdjacencyCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Adjacency, AdjacencyKey>, ByteDataTranslator, EidTranslator,
        JvppReplyConsumer {

    public AdjacencyCustomizer(@Nonnull final FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Adjacency> id,
                                       @Nonnull final Adjacency dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        try {
            addDelAdjacency(true, id, dataAfter);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
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
            addDelAdjacency(false, id, dataBefore);
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }
    }

    private void addDelAdjacency(boolean add, final InstanceIdentifier<Adjacency> id, Adjacency data)
            throws TimeoutException, VppBaseCallException {

        checkState(id.firstKeyOf(VniTable.class) != null, "Unable to find parent VNI for {}", id);
        final int vni = id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue();

        EidType localEidType = getEidType(data.getLocalEid());
        EidType remoteEidType = getEidType(data.getRemoteEid());

        checkArgument(localEidType ==
                remoteEidType, "Local[%s] and Remote[%s] eid types must be the same", localEidType, remoteEidType);

        LispAddDelAdjacency request = new LispAddDelAdjacency();

        request.isAdd = booleanToByte(add);
        request.seid = getEidAsByteArray(data.getLocalEid());
        request.seidLen = getPrefixLength(data.getLocalEid());
        request.deid = getEidAsByteArray(data.getRemoteEid());
        request.deidLen = getPrefixLength(data.getRemoteEid());
        request.eidType = (byte) localEidType.getValue();
        request.vni = vni;

        getReply(getFutureJVpp().lispAddDelAdjacency(request).toCompletableFuture());
    }
}
