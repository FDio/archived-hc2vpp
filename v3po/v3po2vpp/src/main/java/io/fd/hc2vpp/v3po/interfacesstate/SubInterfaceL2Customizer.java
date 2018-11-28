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

package io.fd.hc2vpp.v3po.interfacesstate;

import static io.fd.hc2vpp.v3po.util.SubInterfaceUtils.getSubInterfaceName;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.l2.state.attributes.interconnection.BridgeBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.l2.state.attributes.interconnection.XconnectBased;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.l2.state.attributes.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.l2.state.attributes.L2Builder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading vlan sub interface L2 operational state
 */
public class SubInterfaceL2Customizer
        implements InitializingReaderCustomizer<L2, L2Builder> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceL2Customizer.class);
    private final InterconnectionReadUtils icReadUtils;

    public SubInterfaceL2Customizer(@Nonnull final FutureJVppCore futureJVppCore,
                                    @Nonnull final NamingContext interfaceContext,
                                    @Nonnull final NamingContext bridgeDomainContext,
                                    @Nonnull final InterfaceCacheDumpManager dumpManager) {
        this.icReadUtils =
                new InterconnectionReadUtils(futureJVppCore, interfaceContext, bridgeDomainContext, dumpManager);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final L2 readValue) {
        ((SubInterfaceBuilder) parentBuilder).setL2(readValue);
    }

    @Nonnull
    @Override
    public L2Builder getBuilder(@Nonnull final InstanceIdentifier<L2> id) {
        return new L2Builder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2Builder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for sub-interface L2: {}", id);
        final InterfaceKey parentInterfacekey = id.firstKeyOf(Interface.class);
        final SubInterfaceKey subInterfacekey = id.firstKeyOf(SubInterface.class);
        final String subInterfaceName =
                getSubInterfaceName(parentInterfacekey.getName(), subInterfacekey.getIdentifier().intValue());

        builder.setInterconnection(icReadUtils.readInterconnection(id, subInterfaceName, ctx));
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.l2.config.attributes.L2> init(
            @Nonnull final InstanceIdentifier<L2> id,
            @Nonnull final L2 readValue,
            @Nonnull final ReadContext ctx) {

        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.l2.config.attributes.L2Builder
                builder =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.l2.config.attributes.L2Builder();

        if (readValue.getInterconnection() instanceof XconnectBased) {
            XconnectBased state = (XconnectBased) readValue.getInterconnection();
            builder.setInterconnection(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.l2.config.attributes.interconnection.XconnectBasedBuilder()
                            .setXconnectOutgoingInterface(state.getXconnectOutgoingInterface())
                            .build());
        } else {
            BridgeBased state = (BridgeBased) readValue.getInterconnection();
            builder.setInterconnection(
                    new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.l2.config.attributes.interconnection.BridgeBasedBuilder()
                            .setBridgeDomain(state.getBridgeDomain())
                            .setBridgedVirtualInterface(state.isBridgedVirtualInterface())
                            .setSplitHorizonGroup(state.getSplitHorizonGroup())
                            .build());
        }

        return Initialized.create(getCfgId(id), builder.setRewrite(readValue.getRewrite()).build());
    }

    static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.l2.config.attributes.L2> getCfgId(
            final InstanceIdentifier<L2> id) {
        return SubInterfaceCustomizer.getCfgId(RWUtils.cutId(id, SubInterface.class))
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.sub._interface.l2.config.attributes.L2.class);
    }
}
