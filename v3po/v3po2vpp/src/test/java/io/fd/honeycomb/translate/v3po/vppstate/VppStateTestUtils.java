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

package io.fd.honeycomb.translate.v3po.vppstate;

import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.registry.ReaderRegistry;
import io.fd.honeycomb.translate.util.read.registry.CompositeReaderRegistryBuilder;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.Version;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

final class VppStateTestUtils {

    private static InstanceIdentifier<BridgeDomains> bridgeDomainsId;

    public VppStateTestUtils() {
    }

    /**
     * Create root VppState reader with all its children wired.
     */
    static ReaderRegistry getVppStateReader(@Nonnull final FutureJVpp jVpp,
                                            @Nonnull final NamingContext bdContext) {
        final CompositeReaderRegistryBuilder registry = new CompositeReaderRegistryBuilder();

        // VppState(Structural)
        final InstanceIdentifier<VppState> vppStateId = InstanceIdentifier.create(VppState.class);
        registry.addStructuralReader(vppStateId, VppStateBuilder.class);
        //  Version
        // Wrap with keepalive reader to detect connection issues
        // TODO keepalive reader wrapper relies on VersionReaderCustomizer (to perform timeout on reads)
        // Once readers+customizers are asynchronous, pull the timeout to keepalive executor so that keepalive wrapper
        // is truly generic
        registry.add(new GenericReader<>(vppStateId.child(Version.class), new VersionCustomizer(jVpp)));
        //  BridgeDomains(Structural)
        bridgeDomainsId = vppStateId.child(BridgeDomains.class);
        registry.addStructuralReader(bridgeDomainsId, BridgeDomainsBuilder.class);
        //   BridgeDomain
        registry.add(getBridgeDomainReader(jVpp, bdContext));
        return registry.build();
    }

    static GenericListReader<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> getBridgeDomainReader(
            final @Nonnull FutureJVpp jVpp, final @Nonnull NamingContext bdContext) {
        final InstanceIdentifier<BridgeDomain> bridgeDomainId = bridgeDomainsId.child(BridgeDomain.class);
        return new GenericListReader<>(bridgeDomainId, new BridgeDomainCustomizer(jVpp, bdContext));
    }
}
