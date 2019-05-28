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

package io.fd.hc2vpp.v3po.read;

import io.fd.hc2vpp.v3po.read.cache.InterfaceCacheDumpManager;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.jvpp.core.dto.SwInterfaceDetails;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VppInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.EthernetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class EthernetCustomizer
        implements InitializingReaderCustomizer<Ethernet, EthernetBuilder>, InterfaceDataTranslator {

    private final InterfaceCacheDumpManager dumpManager;

    public EthernetCustomizer(@Nonnull final InterfaceCacheDumpManager dumpManager) {
        this.dumpManager = dumpManager;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final Ethernet readValue) {
        ((VppInterfaceAugmentationBuilder) parentBuilder).setEthernet(readValue);
    }

    @Nonnull
    @Override
    public EthernetBuilder getBuilder(@Nonnull InstanceIdentifier<Ethernet> id) {
        return new EthernetBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Ethernet> id,
                                      @Nonnull final EthernetBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final SwInterfaceDetails iface = dumpManager.getInterfaceDetail(id, ctx, key.getName());

        if (iface.linkMtu != 0) {
            // Read physical payload MTU (link_mtu) if given.
            // VPP since 18.07 supports also setting MTUs for software interfaces,
            // but these are not supported by HC (TODO: HC2VPP-355).
            // More details:
            // https://git.fd.io/vpp/tree/src/vnet/MTU.md
            builder.setMtu((int) iface.linkMtu);
        }

        switch (iface.linkDuplex) {
            case 1:
                builder.setDuplex(Ethernet.Duplex.Half);
                break;
            case 2:
                builder.setDuplex(Ethernet.Duplex.Full);
                break;
            default:
                break;
        }
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Ethernet> init(
            @Nonnull final InstanceIdentifier<Ethernet> id,
            @Nonnull final Ethernet readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.EthernetBuilder()
                        .setMtu(readValue.getMtu())
                        .build());
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Ethernet> getCfgId(
            final InstanceIdentifier<Ethernet> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Ethernet.class);
    }
}
