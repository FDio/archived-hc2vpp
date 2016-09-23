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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.ByteDataTranslator;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.LispEidTableAddDelMap;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Customizer for {@code TableId}
 */
public class VniTableCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<VniTable, VniTableKey>, ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(VniTableCustomizer.class);

    public VniTableCustomizer(FutureJVppCore futureJvpp) {
        super(futureJvpp);
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<VniTable> id, VniTable dataAfter, WriteContext writeContext)
            throws WriteFailedException {

        checkNotNull(dataAfter.getTableId(), "VRF cannot be null");
        checkNotNull(dataAfter.getVirtualNetworkIdentifier(), "VNI cannot be null");

        LOG.debug("Writing {}", id);

        try {
            addDelMap(true, dataAfter.getVirtualNetworkIdentifier().intValue(), dataAfter.getTableId().intValue());
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        LOG.debug("Write of {} successful", id);
    }

    @Override
    public void updateCurrentAttributes(InstanceIdentifier<VniTable> id, VniTable dataBefore, VniTable dataAfter,
                                        WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<VniTable> id, VniTable dataBefore, WriteContext writeContext)
            throws WriteFailedException {
        checkNotNull(dataBefore.getTableId(), "VRF cannot be null");
        checkNotNull(dataBefore.getVirtualNetworkIdentifier(), "VNI cannot be null");

        LOG.debug("Removing {}", id);

        try {
            addDelMap(false, dataBefore.getVirtualNetworkIdentifier().intValue(), dataBefore.getTableId().intValue());
        } catch (TimeoutException | VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }

        LOG.debug("Remove  of {} successful", id);
    }

    private void addDelMap(boolean isAdd, int vni, int vrf) throws TimeoutException, VppBaseCallException {

        LispEidTableAddDelMap request = new LispEidTableAddDelMap();

        request.isAdd = booleanToByte(isAdd);
        request.vni = vni;
        request.dpTable = vrf;
        request.isL2 = 0;

        getReply(getFutureJVpp().lispEidTableAddDelMap(request).toCompletableFuture());
    }
}
