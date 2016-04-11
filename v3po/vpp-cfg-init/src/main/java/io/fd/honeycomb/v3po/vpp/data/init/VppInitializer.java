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

package io.fd.honeycomb.v3po.vpp.data.init;

import io.fd.honeycomb.v3po.data.ModifiableDataTree;
import io.fd.honeycomb.v3po.translate.read.ReaderRegistry;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomains;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppInitializer extends AbstractDataTreeConverter<VppState, Vpp> {
    private static final Logger LOG = LoggerFactory.getLogger(VppInitializer.class);

    public VppInitializer(@Nonnull final ReaderRegistry readerRegistry,
                          @Nonnull final ModifiableDataTree configDataTree,
                          @Nonnull final BindingNormalizedNodeSerializer serializer) {
        super(readerRegistry, configDataTree, serializer, InstanceIdentifier.create(VppState.class), InstanceIdentifier.create(Vpp.class) );
    }

    @Override
    public void close() throws Exception {
        // NOP
        LOG.debug("VppStateInitializer.close()");
        // FIXME implement delete
    }

    @Override
    protected Vpp convert(final VppState operationalData) {
        LOG.debug("VppStateInitializer.convert()");
        final BridgeDomains bridgeDomains = operationalData.getBridgeDomains();
        final List<BridgeDomain> bridgeDomainList = bridgeDomains.getBridgeDomain();

        VppBuilder vppBuilder = new VppBuilder();
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomainsBuilder
                bdsBuilder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomainsBuilder();
        final List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain>
                listOfBDs = new ArrayList<>();

        // TODO use reflexion
        for (BridgeDomain bd : bridgeDomainList) {
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder
                    bdBuilder =
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder();
            bdBuilder.setLearn(bd.isLearn());
            bdBuilder.setUnknownUnicastFlood(bd.isUnknownUnicastFlood());
            bdBuilder.setArpTermination(bd.isArpTermination());
            bdBuilder.setFlood(bd.isFlood());
            bdBuilder.setForward(bd.isForward());
            bdBuilder.setKey(new BridgeDomainKey(bd.getKey().getName()));
            // TODO bdBuilder.setL2Fib(bd.getL2Fib());
            bdBuilder.setName(bd.getName());
            listOfBDs.add(bdBuilder.build());
        }

        bdsBuilder.setBridgeDomain(listOfBDs);
        vppBuilder.setBridgeDomains(bdsBuilder.build());
        return vppBuilder.build();
    }
}
