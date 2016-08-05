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
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.EthernetBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.SwInterfaceDetails;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EthernetCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<Ethernet, EthernetBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(EthernetCustomizer.class);
    private NamingContext interfaceContext;

    public EthernetCustomizer(@Nonnull final FutureJVppCore jvpp,
                              @Nonnull final NamingContext interfaceContext) {
        super(jvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final Ethernet readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setEthernet(readValue);
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
        final SwInterfaceDetails iface = InterfaceUtils.getVppInterfaceDetails(getFutureJVpp(), id, key.getName(),
                interfaceContext.getIndex(key.getName(), ctx.getMappingContext()), ctx.getModificationCache());

        if(iface.linkMtu != 0) {
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
}
