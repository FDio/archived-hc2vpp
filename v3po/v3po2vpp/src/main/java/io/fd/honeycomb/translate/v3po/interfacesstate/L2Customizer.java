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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces.state._interface.L2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.base.attributes.Interconnection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.base.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.base.attributes.interconnection.BridgeBasedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.base.attributes.interconnection.XconnectBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.l2.base.attributes.interconnection.XconnectBasedBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading ietf-interfaces:interfaces-state/interface/iface_name/v3po:l2
 */
public class L2Customizer extends FutureJVppCustomizer implements InitializingReaderCustomizer<L2, L2Builder> {

    private static final Logger LOG = LoggerFactory.getLogger(L2Customizer.class);
    private final InterconnectionReadUtils icReadUtils;

    public L2Customizer(@Nonnull final FutureJVppCore futureJVppCore,
                        @Nonnull final NamingContext interfaceContext,
                        @Nonnull final NamingContext bridgeDomainContext) {
        super(futureJVppCore);
        this.icReadUtils = new InterconnectionReadUtils(futureJVppCore, interfaceContext, bridgeDomainContext);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final L2 readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setL2(readValue);
    }

    @Nonnull
    @Override
    public L2Builder getBuilder(@Nonnull final InstanceIdentifier<L2> id) {
        return new L2Builder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2Builder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        LOG.debug("Reading attributes for L2: {}", id);
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final String ifaceName = key.getName();
        builder.setInterconnection(icReadUtils.readInterconnection(id, ifaceName, ctx));
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.L2> init(
            @Nonnull final InstanceIdentifier<L2> id,
            @Nonnull final L2 readValue,
            @Nonnull final ReadContext ctx) {
        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.L2Builder
                l2Builder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.L2Builder();

        final Interconnection interconnection = readValue.getInterconnection();
        if (interconnection != null) {
            if (interconnection instanceof XconnectBased) {
                final XconnectBasedBuilder xconnectBasedBuilder = new XconnectBasedBuilder();
                xconnectBasedBuilder.setXconnectOutgoingInterface(
                        ((XconnectBased) interconnection).getXconnectOutgoingInterface());
                l2Builder.setInterconnection(xconnectBasedBuilder.build());
            } else if (interconnection instanceof BridgeBased) {
                final BridgeBasedBuilder bridgeBasedBuilder = new BridgeBasedBuilder();
                bridgeBasedBuilder.setBridgeDomain(((BridgeBased) interconnection).getBridgeDomain());
                bridgeBasedBuilder
                        .setBridgedVirtualInterface(((BridgeBased) interconnection).isBridgedVirtualInterface());
                bridgeBasedBuilder.setSplitHorizonGroup(((BridgeBased) interconnection).getSplitHorizonGroup());
                l2Builder.setInterconnection(bridgeBasedBuilder.build());
            }
        }

        return Initialized.create(getCfgId(id), l2Builder.build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.L2> getCfgId(
            final InstanceIdentifier<L2> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.L2.class);
    }
}
