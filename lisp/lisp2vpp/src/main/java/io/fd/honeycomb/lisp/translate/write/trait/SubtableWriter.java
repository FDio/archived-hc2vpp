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

package io.fd.honeycomb.lisp.translate.write.trait;


import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.LispEidTableAddDelMap;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;

/**
 * Trait providing logic for writing subtables
 */
public interface SubtableWriter extends ByteDataTranslator, JvppReplyConsumer {
    int DEFAULT_VNI = 0;

    /**
     * Writes mapping from {@link VniTable}
     * to {@link VrfSubtable} or
     * {@link BridgeDomainSubtable}
     *
     * @param addDel  true if add,delete otherwise
     * @param vni     {@link VniTable} ID
     * @param tableId if <b>isL2</b> is true, than bridge domain subtable id,else vrf subtable id
     * @param isL2    indicates whether (false) writing to L3 vrfSubtrable of (true) L2 bridgeDomainSubtrable
     */
    default void addDelSubtableMapping(@Nonnull final FutureJVppCore vppApi, final boolean addDel, final int vni,
                                       final int tableId,
                                       final boolean isL2,
                                       final Logger logger) throws TimeoutException, VppBaseCallException {

        if (vni == DEFAULT_VNI) {
            // attempt to write subtable with default vni mapping(it does'nt make sense and it should'nt be possible)
            // also allows to enable lisp without defining default mapping in request
            logger.info("An attempt to write subtable[id = {}] with default vni {} was detected, ignoring write",
                    tableId, DEFAULT_VNI);
            return;
        }

        checkNotNull(vppApi, "VPP Api refference cannot be null");

        LispEidTableAddDelMap request = new LispEidTableAddDelMap();

        request.isAdd = booleanToByte(addDel);
        request.vni = vni;
        request.dpTable = tableId;
        request.isL2 = booleanToByte(isL2);

        getReply(vppApi.lispEidTableAddDelMap(request).toCompletableFuture());
    }

    default int extractVni(@Nonnull final InstanceIdentifier<? extends ChildOf<VniTable>> id) {
        return checkNotNull(
                checkNotNull(id, "Identifier cannot be null").firstKeyOf(VniTable.class),
                "Parent VNI id not defined").getVirtualNetworkIdentifier().intValue();
    }
}
