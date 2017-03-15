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

package io.fd.hc2vpp.l3.write.ipv6.subinterface;


import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.utils.ip.write.IpWriter;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170315.sub._interface.ip6.attributes.ipv6.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170315.sub._interface.ip6.attributes.ipv6.AddressKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceIpv6AddressCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Address, AddressKey>, IpWriter {

    private final NamingContext interfaceContext;

    public SubInterfaceIpv6AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                             @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = checkNotNull(interfaceContext, "interface context should not be null");
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<Address> id, Address dataAfter, WriteContext writeContext)
            throws WriteFailedException {
        setAddress(true, id, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(InstanceIdentifier<Address> id, Address dataBefore, Address dataAfter,
                                        WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<Address> id, Address dataBefore, WriteContext writeContext)
            throws WriteFailedException {
        setAddress(false, id, dataBefore, writeContext);
    }

    private void setAddress(boolean add,
                            final InstanceIdentifier<Address> id,
                            final Address address,
                            final WriteContext writeContext) throws WriteFailedException {

        addDelAddress(getFutureJVpp(), add, id,
                subInterfaceIndex(id, interfaceContext, writeContext.getMappingContext()), address.getIp(),
                address.getPrefixLength().byteValue());
    }
}
