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

package io.fd.honeycomb.translate.v3po.interfacesstate.ip;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.fd.honeycomb.translate.v3po.interfacesstate.ip.Ipv4ReadUtils.dumpAddresses;
import static io.fd.honeycomb.translate.v3po.interfacesstate.ip.Ipv4ReadUtils.findIpAddressDetailsByIp;
import static io.fd.honeycomb.translate.v3po.interfacesstate.ip.Ipv4ReadUtils.getAllIpv4AddressIds;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.IpAddressDetails;
import org.openvpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read customizer for interface Ipv4 addresses.
 */
public class Ipv4AddressCustomizer extends FutureJVppCustomizer
    implements ListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4AddressCustomizer.class);

    private final NamingContext interfaceContext;

    public Ipv4AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore, @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    @Nonnull
    public AddressBuilder getBuilder(@Nonnull InstanceIdentifier<Address> id) {
        return new AddressBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Address> id, @Nonnull AddressBuilder builder,
                                      @Nonnull ReadContext ctx)
        throws ReadFailedException {
        LOG.debug("Reading attributes for interface address: {}", id);

        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, ctx.getMappingContext());
        final Optional<IpAddressDetailsReplyDump> dumpOptional =
            dumpAddresses(getFutureJVpp(), id, interfaceName, interfaceIndex, ctx);

        final Optional<IpAddressDetails> ipAddressDetails =
            findIpAddressDetailsByIp(dumpOptional, id.firstKeyOf(Address.class).getIp());

        if (ipAddressDetails.isPresent()) {
            final IpAddressDetails detail = ipAddressDetails.get();
            builder.setIp(TranslateUtils.arrayToIpv4AddressNoZone(detail.ip))
                .setSubnet(new PrefixLengthBuilder().setPrefixLength(Short.valueOf(detail.prefixLength)).build());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Attributes for {} interface (id={}) address {} successfully read: {}",
                    interfaceName, interfaceIndex, id, builder.build());
            }
        }
    }

    @Override
    public List<AddressKey> getAllIds(@Nonnull InstanceIdentifier<Address> id, @Nonnull ReadContext ctx)
        throws ReadFailedException {
        LOG.debug("Reading list of keys for interface addresses: {}", id);

        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, ctx.getMappingContext());
        final Optional<IpAddressDetailsReplyDump> dumpOptional =
            dumpAddresses(getFutureJVpp(), id, interfaceName, interfaceIndex, ctx);

        return getAllIpv4AddressIds(dumpOptional, AddressKey::new);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Address> readData) {
        ((Ipv4Builder) builder).setAddress(readData);
    }

}