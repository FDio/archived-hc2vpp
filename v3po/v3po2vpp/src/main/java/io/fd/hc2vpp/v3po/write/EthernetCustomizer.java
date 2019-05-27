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

package io.fd.hc2vpp.v3po.write;

import io.fd.hc2vpp.common.translate.util.AbstractInterfaceTypeCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.HwInterfaceSetMtu;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev180703.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EthernetCustomizer extends AbstractInterfaceTypeCustomizer<Ethernet> implements JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(EthernetCustomizer.class);
    private final NamingContext interfaceContext;

    public EthernetCustomizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return EthernetCsmacd.class;
    }

    @Override
    public void writeInterface(@Nonnull final InstanceIdentifier<Ethernet> id,
                                       @Nonnull final Ethernet dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        setEthernetAttributes(id, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Ethernet> id,
                                        @Nonnull final Ethernet dataBefore, @Nonnull final Ethernet dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        setEthernetAttributes(id, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Ethernet> id,
                                        @Nonnull final Ethernet dataBefore, @Nonnull final WriteContext writeContext)
        throws WriteFailedException.DeleteFailedException {
        throw new WriteFailedException.DeleteFailedException(id,
            new UnsupportedOperationException("Removing ethernet container is not supported"));
    }

    private void setEthernetAttributes(@Nonnull final InstanceIdentifier<Ethernet> id,
                                       @Nonnull final Ethernet dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final String name = id.firstKeyOf(Interface.class).getName();
        final int index = interfaceContext.getIndex(name, writeContext.getMappingContext());
        LOG.debug("Setting Ethernet attributes for interface: {}, {}. Ethernet: {}", name, index, dataAfter);

        // Set the physical payload MTU. I.e. not including L2 overhead.
        // Setting the hardware MTU will program the NIC.
        // Setting MTU for software interfaces is currently not supported (TODO: HC2VPP-355).
        // More details:
        // https://git.fd.io/vpp/tree/src/vnet/MTU.md
        final HwInterfaceSetMtu request = new HwInterfaceSetMtu();
        request.swIfIndex = index;
        request.mtu = dataAfter.getMtu().shortValue();
        getReplyForWrite(getFutureJVpp().hwInterfaceSetMtu(request).toCompletableFuture(), id);
        LOG.debug("Ethernet attributes set successfully for: {}, {}. Ethernet: {}", name, index, dataAfter);
    }
}
