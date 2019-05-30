/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

import io.fd.hc2vpp.common.translate.util.MacTranslator;
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
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.Loopback;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.LoopbackBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class LoopbackCustomizer
        implements InitializingReaderCustomizer<Loopback, LoopbackBuilder>, MacTranslator, InterfaceDataTranslator {

    private final InterfaceCacheDumpManager dumpManager;

    public LoopbackCustomizer(@Nonnull final InterfaceCacheDumpManager dumpManager) {
        this.dumpManager = dumpManager;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final Loopback readValue) {
        ((VppInterfaceAugmentationBuilder) parentBuilder).setLoopback(readValue);
    }

    @Nonnull
    @Override
    public LoopbackBuilder getBuilder(@Nonnull InstanceIdentifier<Loopback> id) {
        return new LoopbackBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Loopback> id,
                                      @Nonnull final LoopbackBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        if (!org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.Loopback.class
                .equals(getInterfaceType(id.firstKeyOf(Interface.class).getName()))) {
            return;
        }

        final SwInterfaceDetails iface = dumpManager.getInterfaceDetail(id, ctx, key.getName());

        if (iface.l2AddressLength == 6) {
            builder.setMac(toPhysAddress(iface.l2Address));
        }
    }

    @Override
    public Initialized<Loopback> init(@Nonnull final InstanceIdentifier<Loopback> id, @Nonnull final Loopback readValue,
                                      @Nonnull final ReadContext ctx) {
        if (org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.Loopback.class
                .equals(getInterfaceType(id.firstKeyOf(Interface.class).getName()))) {
            return Initialized.create(getCfgId(id), new LoopbackBuilder().setMac(readValue.getMac()).build());
        } else {
            return Initialized.create(getCfgId(id), new LoopbackBuilder().build());
        }
    }

    private InstanceIdentifier<Loopback> getCfgId(final InstanceIdentifier<Loopback> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(VppInterfaceAugmentation.class)
                .child(Loopback.class);
    }
}
