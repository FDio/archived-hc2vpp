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

package io.fd.honeycomb.v3po.impl.vppstate;

import io.fd.honeycomb.v3po.impl.trans.r.ChildVppReader;
import io.fd.honeycomb.v3po.impl.trans.r.impl.CompositeChildVppReader;
import io.fd.honeycomb.v3po.impl.trans.r.impl.CompositeListVppReader;
import io.fd.honeycomb.v3po.impl.trans.r.impl.CompositeRootVppReader;
import io.fd.honeycomb.v3po.impl.trans.r.util.ReflexiveChildReaderCustomizer;
import io.fd.honeycomb.v3po.impl.trans.r.util.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.impl.trans.r.util.VppRWUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.openvpp.vppjapi.vppApi;

final class VppStateUtils {

    public VppStateUtils() {}

    /**
     * Create root VppState reader with all its children wired
     */
    static CompositeRootVppReader<VppState, VppStateBuilder> getVppStateReader(@Nonnull final vppApi vppApi) {

        final ChildVppReader<Version> versionReader = new CompositeChildVppReader<>(
            Version.class, new VersionCustomizer(vppApi));

        final CompositeListVppReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder>
            bridgeDomainReader = new CompositeListVppReader<>(
            BridgeDomain.class,
            new BridgeDomainCustomizer(vppApi));

        final ChildVppReader<BridgeDomains> bridgeDomainsReader = new CompositeChildVppReader<>(
            BridgeDomains.class,
            VppRWUtils.singletonChildReaderList(bridgeDomainReader),
            new ReflexiveChildReaderCustomizer<>(BridgeDomainsBuilder.class));

        final List<ChildVppReader<? extends ChildOf<VppState>>> childVppReaders = new ArrayList<>();
        childVppReaders.add(versionReader);
        childVppReaders.add(bridgeDomainsReader);

        return new CompositeRootVppReader<>(
            VppState.class,
            childVppReaders,
            VppRWUtils.<VppState>emptyAugReaderList(),
            new ReflexiveRootReaderCustomizer<>(VppStateBuilder.class));
    }
}
