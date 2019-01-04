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

package io.fd.hc2vpp.v3po.factory;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.l2state.BridgeDomainCustomizer;
import io.fd.hc2vpp.v3po.l2state.L2FibEntryCustomizer;
import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.BridgeDomainsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.BridgeDomainsStateBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.bridge.domains.state.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.l2.fib.attributes.L2FibTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.l2.fib.attributes.L2FibTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181008.l2.fib.attributes.l2.fib.table.L2FibEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class L2StateHoneycombReaderFactory implements ReaderFactory {

    private final FutureJVppCore jVpp;
    private final NamingContext ifcCtx;
    private final NamingContext bdCtx;

    @Inject
    public L2StateHoneycombReaderFactory(final FutureJVppCore jVpp,
                                         @Named("interface-context") final NamingContext ifcCtx,
                                         @Named("bridge-domain-context") final NamingContext bdCtx) {
        this.jVpp = jVpp;
        this.ifcCtx = ifcCtx;
        this.bdCtx = bdCtx;
    }

    @Override
    public void init(final ModifiableReaderRegistryBuilder registry) {
        //  BridgeDomains(Structural)
        final InstanceIdentifier<BridgeDomainsState> bridgeDomainsId = InstanceIdentifier.create(BridgeDomainsState.class);
        registry.addStructuralReader(bridgeDomainsId, BridgeDomainsStateBuilder.class);
        //   BridgeDomain
        final InstanceIdentifier<BridgeDomain> bridgeDomainId = bridgeDomainsId.child(BridgeDomain.class);
        registry.add(new GenericInitListReader<>(bridgeDomainId, new BridgeDomainCustomizer(jVpp, bdCtx)));
        //    L2FibTable(Structural)
        final InstanceIdentifier<L2FibTable> l2FibTableId = bridgeDomainId.child(L2FibTable.class);
        registry.addStructuralReader(l2FibTableId, L2FibTableBuilder.class);
        //     L2FibEntry
        registry.add(new GenericInitListReader<>(l2FibTableId.child(L2FibEntry.class),
                new L2FibEntryCustomizer(jVpp, bdCtx, ifcCtx)));
    }
}
