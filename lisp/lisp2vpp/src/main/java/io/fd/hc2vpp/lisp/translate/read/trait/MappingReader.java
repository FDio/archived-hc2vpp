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

package io.fd.hc2vpp.lisp.translate.read.trait;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV4;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.IPV6;
import static io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams.EidType.MAC;

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.read.dump.executor.params.MappingsDumpParams;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.core.dto.OneEidTableDetails;
import io.fd.vpp.jvpp.core.dto.OneEidTableDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.OneEidTableDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Trait providing predicates to filter mappings to respective sub-tables
 */
public interface MappingReader extends JvppReplyConsumer {

    Predicate<OneEidTableDetails> BRIDGE_DOMAIN_MAPPINGS_ONLY =
            (OneEidTableDetails detail) -> detail.eidType == MAC.getVppTypeBinding();

    Predicate<OneEidTableDetails> VRF_MAPPINGS_ONLY =
            (OneEidTableDetails detail) -> detail.eidType == IPV4.getVppTypeBinding() || detail.eidType == IPV6.getVppTypeBinding();

    default Predicate<OneEidTableDetails> subtableFilterForLocalMappings(
            @Nonnull final InstanceIdentifier<LocalMapping> identifier) {

        if (identifier.firstIdentifierOf(VrfSubtable.class) != null) {
            return VRF_MAPPINGS_ONLY;
        } else if (identifier.firstIdentifierOf(BridgeDomainSubtable.class) != null) {
            return BRIDGE_DOMAIN_MAPPINGS_ONLY;
        } else {
            throw new IllegalArgumentException("Cannot determine mappings predicate for " + identifier);
        }
    }

    default Predicate<OneEidTableDetails> subtableFilterForRemoteMappings(
            @Nonnull final InstanceIdentifier<RemoteMapping> identifier) {

        if (identifier.firstIdentifierOf(VrfSubtable.class) != null) {
            return VRF_MAPPINGS_ONLY;
        } else if (identifier.firstIdentifierOf(BridgeDomainSubtable.class) != null) {
            return BRIDGE_DOMAIN_MAPPINGS_ONLY;
        } else {
            throw new IllegalArgumentException("Cannot determine mappings predicate for " + identifier);
        }
    }

    default EntityDumpExecutor<OneEidTableDetailsReplyDump, MappingsDumpParams> createMappingDumpExecutor(
            @Nonnull final FutureJVppCore vppApi) {
        return (identifier, params) -> {
            checkNotNull(params, "Params for dump request not present");

            OneEidTableDump request = new OneEidTableDump();
            request.eid = params.getEid();
            request.eidSet = params.getEidSet();
            request.eidType = params.getEidType();
            request.prefixLength = params.getPrefixLength();
            request.vni = params.getVni();
            request.filter = params.getFilter();

            return getReplyForRead(vppApi.oneEidTableDump(request).toCompletableFuture(), identifier);
        };
    }
}
