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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate;

import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.EthernetBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceDetails;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VppInterfaceStateCustomizer extends FutureJVppCustomizer
        implements ChildReaderCustomizer<VppInterfaceStateAugmentation, VppInterfaceStateAugmentationBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(VppInterfaceStateCustomizer.class);

    public VppInterfaceStateCustomizer(@Nonnull final FutureJVpp jvpp) {
        super(jvpp);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder,
                      @Nonnull VppInterfaceStateAugmentation readValue) {
        ((InterfaceBuilder) parentBuilder).addAugmentation(VppInterfaceStateAugmentation.class, readValue);
    }

    @Nonnull
    @Override
    public VppInterfaceStateAugmentationBuilder getBuilder(
            @Nonnull InstanceIdentifier<VppInterfaceStateAugmentation> id) {
        return new VppInterfaceStateAugmentationBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VppInterfaceStateAugmentation> id,
                                      @Nonnull final VppInterfaceStateAugmentationBuilder builder,
                                      @Nonnull final Context ctx) throws ReadFailedException {

        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final SwInterfaceDetails iface;
        try {
            iface = InterfaceUtils.getVppInterfaceDetails(getFutureJVpp(), key);
        } catch (Exception e) {
            throw new ReadFailedException(id, e);
        }

        final EthernetBuilder ethernet = new EthernetBuilder();
        ethernet.setMtu((int) iface.linkMtu);
        switch (iface.linkDuplex) {
            case 1:
                ethernet.setDuplex(Ethernet.Duplex.Half);
                break;
            case 2:
                ethernet.setDuplex(Ethernet.Duplex.Full);
                break;
            default:
                break;
        }

        builder.setEthernet(ethernet.build());
    }
}
