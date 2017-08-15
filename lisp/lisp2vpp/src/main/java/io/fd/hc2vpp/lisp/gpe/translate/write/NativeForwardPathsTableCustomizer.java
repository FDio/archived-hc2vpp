/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.GpeAddDelIface;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses api to create gpe interfaces, which creates fib table as by-product.
 * There is currently no other lisp-specific way to define fib table, as native paths expects existing table id
 */
public class NativeForwardPathsTableCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<NativeForwardPathsTable, NativeForwardPathsTableKey>, ByteDataTranslator,
        JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(NativeForwardPathsTableCustomizer.class);

    public NativeForwardPathsTableCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<NativeForwardPathsTable> id,
                                       @Nonnull final NativeForwardPathsTable dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        createFibTable(id, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<NativeForwardPathsTable> id,
                                        @Nonnull final NativeForwardPathsTable dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        deleteFibTable(id);
    }

    private void createFibTable(final InstanceIdentifier<NativeForwardPathsTable> id,
                                final NativeForwardPathsTable data) throws WriteFailedException {
        getReplyForCreate(getFutureJVpp().gpeAddDelIface(getRequest(true, id)).toCompletableFuture(), id, data);
    }

    private void deleteFibTable(final InstanceIdentifier<NativeForwardPathsTable> id) throws WriteFailedException {
        getReplyForDelete(getFutureJVpp().gpeAddDelIface(getRequest(false, id)).toCompletableFuture(), id);
    }

    /**
     * Maps dpTable and vni to tableId,this also allows to dump lisp specific tables by dumping vni's
     */
    private GpeAddDelIface getRequest(final boolean add, final InstanceIdentifier<NativeForwardPathsTable> id) {
        GpeAddDelIface request = new GpeAddDelIface();
        request.isL2 = 0;
        // expects reversed order
        request.dpTable = tableId(id);
        request.vni = request.dpTable; // vni must be unique for every table
        request.isAdd = booleanToByte(add);
        return request;
    }

    static int tableId(final InstanceIdentifier<?> id) {
        return id.firstKeyOf(NativeForwardPathsTable.class).getTableId().intValue();
    }
}
