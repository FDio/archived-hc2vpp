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

package io.fd.honeycomb.v3po.translate.v3po.vppstate;

import io.fd.honeycomb.v3po.translate.impl.read.CompositeChildReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeListReader;
import io.fd.honeycomb.v3po.translate.impl.read.CompositeRootReader;
import io.fd.honeycomb.v3po.translate.read.ChildReader;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.ReflexiveRootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
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
import org.openvpp.jvpp.future.FutureJVpp;

final class VppStateTestUtils {

    public VppStateTestUtils() {
    }

    /**
     * Create root VppState reader with all its children wired
     */
    static CompositeRootReader<VppState, VppStateBuilder> getVppStateReader(@Nonnull final FutureJVpp futureJVpp,
                                                                            @Nonnull final NamingContext bdContext) {

        final ChildReader<Version> versionReader = new CompositeChildReader<>(
            Version.class, new VersionCustomizer(futureJVpp));

        final CompositeListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> bridgeDomainReader =
            getBridgeDomainReader(futureJVpp, bdContext);

        final ChildReader<BridgeDomains> bridgeDomainsReader = new CompositeChildReader<>(
            BridgeDomains.class,
            RWUtils.singletonChildReaderList(bridgeDomainReader),
            new ReflexiveChildReaderCustomizer<>(BridgeDomainsBuilder.class));

        final List<ChildReader<? extends ChildOf<VppState>>> childVppReaders = new ArrayList<>();
        childVppReaders.add(versionReader);
        childVppReaders.add(bridgeDomainsReader);

        return new CompositeRootReader<>(
            VppState.class,
            childVppReaders,
            RWUtils.<VppState>emptyAugReaderList(),
            new ReflexiveRootReaderCustomizer<>(VppStateBuilder.class));
    }

    static CompositeListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> getBridgeDomainReader(
        final @Nonnull FutureJVpp futureJVpp, @Nonnull final NamingContext bdContext) {
        return new CompositeListReader<>(
            BridgeDomain.class,
            new BridgeDomainCustomizer(futureJVpp, bdContext));
    }
}
