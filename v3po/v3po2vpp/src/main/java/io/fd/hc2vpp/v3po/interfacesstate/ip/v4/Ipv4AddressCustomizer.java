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

package io.fd.hc2vpp.v3po.interfacesstate.ip.v4;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.ip.dump.params.IfaceDumpFilter;
import io.fd.hc2vpp.v3po.interfacesstate.ip.readers.IpAddressReader;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.vpp.jvpp.core.dto.IpAddressDetails;
import io.fd.vpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.Subnet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Read customizer for interface Ipv4 addresses.
 */
public class Ipv4AddressCustomizer extends IpAddressReader
        implements InitializingListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4AddressCustomizer.class);

    public Ipv4AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                 @Nonnull final NamingContext interfaceContext) {
        super(interfaceContext, false, new DumpCacheManagerBuilder<IpAddressDetailsReplyDump, IfaceDumpFilter>()
                .withExecutor(createAddressDumpExecutor(futureJVppCore))
                // Key needs to contain interface ID to distinguish dumps between interfaces
                .withCacheKeyFactory(interfaceScopedCacheKeyFactory(IpAddressDetailsReplyDump.class))
                .build());
    }

    @Override
    @Nonnull
    public AddressBuilder getBuilder(@Nonnull InstanceIdentifier<Address> id) {
        return new AddressBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Address> id, @Nonnull AddressBuilder builder,
                                      @Nonnull ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for interface address: {}", id);
        final Optional<IpAddressDetailsReplyDump> dumpOptional = interfaceAddressDumpSupplier(id, ctx);

        if (!dumpOptional.isPresent() || dumpOptional.get().ipAddressDetails.isEmpty()) {
            return;
        }
        final Optional<IpAddressDetails> ipAddressDetails = findIpv4AddressDetailsByIp(dumpOptional, id.firstKeyOf(Address.class).getIp());

        if (ipAddressDetails.isPresent()) {
            final IpAddressDetails detail = ipAddressDetails.get();
            builder.setIp(arrayToIpv4AddressNoZone(detail.ip))
                    .setSubnet(new PrefixLengthBuilder().setPrefixLength(Short.valueOf(detail.prefixLength)).build());

            if (LOG.isDebugEnabled()) {
                final String interfaceName = id.firstKeyOf(Interface.class).getName();
                final int interfaceIndex = getInterfaceContext().getIndex(interfaceName, ctx.getMappingContext());
                LOG.debug("Attributes for {} interface (id={}) address {} successfully read: {}",
                        interfaceName, interfaceIndex, id, builder.build());
            }
        }
    }

    @Override
    public List<AddressKey> getAllIds(@Nonnull InstanceIdentifier<Address> id, @Nonnull ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading list of keys for interface addresses: {}", id);
        return getAllIpv4AddressIds(interfaceAddressDumpSupplier(id, ctx), AddressKey::new);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Address> readData) {
        ((Ipv4Builder) builder).setAddress(readData);
    }

    @Override
    public Initialized<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address> init(
            @Nonnull final InstanceIdentifier<Address> id, @Nonnull final Address readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id),
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressBuilder()
                        .setIp(readValue.getIp())
                        .setSubnet(getSubnet(readValue))
                        .build());
    }

    private static Subnet getSubnet(final Address address) {
        final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.Subnet
                subnet = address.getSubnet();

        // Only prefix length supported
        Preconditions.checkArgument(
                subnet instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLength);

        return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.address.subnet.PrefixLengthBuilder()
                .setPrefixLength(
                        ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLength) subnet)
                                .getPrefixLength()).build();
    }

    static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address> getCfgId(
            final InstanceIdentifier<Address> id) {
        return InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                .augmentation(Interface1.class)
                .child(Ipv4.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Address.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.AddressKey(id.firstKeyOf(Address.class).getIp()));
    }
}
