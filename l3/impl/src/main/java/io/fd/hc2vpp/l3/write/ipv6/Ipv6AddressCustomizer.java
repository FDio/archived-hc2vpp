/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.l3.write.ipv6;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.utils.ip.write.IpWriter;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv6.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev180222.interfaces._interface.ipv6.AddressKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Ipv6AddressCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Address, AddressKey>, IpWriter {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv6AddressCustomizer.class);

    private static final String LINK_LOCAL_START_MASK = "fe08";
    private final NamingContext interfaceContext;

    public Ipv6AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                 @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Address> id, @Nonnull final Address dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());

        // prevents scenario
        // - vpp has been restarted == cleaned state
        // - hc tries to restore data, which has link-local address in it
        // link layer address is created by vpp(generated) after adding first address, so its present just
        // after adding first address, and attempt to override it during init would cause error -1
        if (dataAfter.getIp().getValue().startsWith(LINK_LOCAL_START_MASK)) {
            LOG.info("An attempt to rewrite link-local address with {} has been detected,ignoring request",
                    dataAfter.getIp());
            return;
        }

        addDelAddress(getFutureJVpp(), true, id, interfaceIndex, dataAfter.getIp(),
                dataAfter.getPrefixLength().byteValue());
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Address> id,
                                        @Nonnull final Address dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, writeContext.getMappingContext());

        addDelAddress(getFutureJVpp(), false, id, interfaceIndex, dataBefore.getIp(),
                dataBefore.getPrefixLength().byteValue());
    }

}
