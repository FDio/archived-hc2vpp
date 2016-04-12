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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.Vpp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomainKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes vpp node in config data tree based on operational state
 */
public class VppInitializer extends AbstractDataTreeConverter<VppState, Vpp> {
    private static final Logger LOG = LoggerFactory.getLogger(VppInitializer.class);

    public VppInitializer(@Nonnull final DataBroker bindingDataBroker) {
        super(bindingDataBroker, InstanceIdentifier.create(VppState.class), InstanceIdentifier.create(Vpp.class));
    }

    @Override
    protected Vpp convert(final VppState operationalData) {
        LOG.debug("VppInitializer.convert()");

        VppBuilder vppBuilder = new VppBuilder();
        BridgeDomainsBuilder bdsBuilder = new BridgeDomainsBuilder();

        bdsBuilder.setBridgeDomain(Lists.transform(operationalData.getBridgeDomains().getBridgeDomain(), CONVERT_BD));
        vppBuilder.setBridgeDomains(bdsBuilder.build());
        return vppBuilder.build();
    }

    private static final Function<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain, BridgeDomain>
            CONVERT_BD =
            new Function<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain, BridgeDomain>() {
                @Nullable
                @Override
                public BridgeDomain apply(
                        @Nullable final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain input) {
                    final BridgeDomainBuilder builder = new BridgeDomainBuilder();
                    builder.setLearn(input.isLearn());
                    builder.setUnknownUnicastFlood(input.isUnknownUnicastFlood());
                    builder.setArpTermination(input.isArpTermination());
                    builder.setFlood(input.isFlood());
                    builder.setForward(input.isForward());
                    builder.setKey(new BridgeDomainKey(input.getKey().getName()));
                    // TODO bdBuilder.setL2Fib(bd.getL2Fib());
                    builder.setName(input.getName());
                    return builder.build();
                }
            };
}
