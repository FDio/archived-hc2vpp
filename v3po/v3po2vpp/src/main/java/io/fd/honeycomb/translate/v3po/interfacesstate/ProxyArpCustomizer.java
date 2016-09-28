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
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.ProxyArp;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyArpCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<ProxyArp,
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.ProxyArpBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyArpCustomizer.class);
    private final NamingContext interfaceContext;

    public ProxyArpCustomizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder,
                      @Nonnull org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105
                              .interfaces.state._interface.ProxyArp readValue) {

        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setProxyArp(readValue);
    }

    @Nonnull
    @Override
    public org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state
            ._interface.ProxyArpBuilder getBuilder(
            @Nonnull InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight
                    .params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.ProxyArp> id) {

        return new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces
                .state._interface.ProxyArpBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight
            .params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.ProxyArp> id,
                                      @Nonnull org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po
                                              .rev150105.interfaces.state._interface.ProxyArpBuilder builder,
                                      @Nonnull ReadContext ctx) throws ReadFailedException {

        //TODO: VPP-225 Implement fully when VPP Proxy ARP read API is available
        final InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = interfaceContext.getIndex(key.getName(), ctx.getMappingContext());
        LOG.warn("Reading of ARP data not (yet) supported by VPP API");
    }
}
