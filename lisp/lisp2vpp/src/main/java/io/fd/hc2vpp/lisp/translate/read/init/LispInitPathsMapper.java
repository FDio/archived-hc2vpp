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
package io.fd.hc2vpp.lisp.translate.read.init;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.DpSubtableGrouping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.lisp.feature.data.grouping.LispFeatureData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

public interface LispInitPathsMapper {

    @Nonnull
    default InstanceIdentifier<LispFeatureData> lispFeaturesBasePath() {
        return InstanceIdentifier.create(Lisp.class).child(LispFeatureData.class);
    }

    @Nonnull
    default InstanceIdentifier<LocatorSet> lispLocatorSetPath(@Nonnull final InstanceIdentifier<?> base) {
        final Optional<LocatorSetKey> key = Optional.ofNullable(base.firstKeyOf(LocatorSet.class));

        checkState(key.isPresent(), "No locator set key present for %s", base);

        return lispFeaturesBasePath().child(LocatorSets.class).child(LocatorSet.class, key.get());
    }

    @Nonnull
    default InstanceIdentifier<? extends DpSubtableGrouping> vniSubtablePath(@Nonnull final InstanceIdentifier<?> base) {
        final Optional<InstanceIdentifier<VrfSubtable>> vrfSubtableIdentifier = Optional.ofNullable(base.firstIdentifierOf(VrfSubtable.class));
        final Optional<InstanceIdentifier<BridgeDomainSubtable>> bridgeDomainSubtableIdentifier = Optional.ofNullable(base.firstIdentifierOf(BridgeDomainSubtable.class));
        final Optional<VniTableKey> vniTableKey = Optional.ofNullable(base.firstKeyOf(VniTable.class));

        checkState(vniTableKey.isPresent(), "No vni key present for %s", base);
        checkState(vrfSubtableIdentifier.isPresent() || bridgeDomainSubtableIdentifier.isPresent(), "No subtable identifiers present in %s", base);

        if (vrfSubtableIdentifier.isPresent()) {
            return lispFeaturesBasePath().child(EidTable.class).child(VniTable.class, vniTableKey.get()).child(VrfSubtable.class);
        } else {
            return lispFeaturesBasePath().child(EidTable.class).child(VniTable.class, vniTableKey.get()).child(BridgeDomainSubtable.class);
        }
    }

    @Nonnull
    default InstanceIdentifier<RemoteMapping> remoteMappingPath(@Nonnull final InstanceIdentifier<?> base) {
        return vniSubtablePath(base)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class, base.firstKeyOf(RemoteMapping.class));
    }
}
