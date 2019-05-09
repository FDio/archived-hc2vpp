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

package io.fd.hc2vpp.l3.read.ipv6.subinterface;


import java.util.Optional;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.l3.utils.ip.read.IfaceDumpFilter;
import io.fd.hc2vpp.l3.utils.ip.read.IpAddressReader;
import io.fd.hc2vpp.v3po.interfacesstate.SubInterfaceCustomizer;
import io.fd.hc2vpp.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.IpAddressDetails;
import io.fd.jvpp.core.dto.IpAddressDetailsReplyDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.Ipv6Builder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.Address;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.AddressBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.ip6.attributes.ipv6.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubInterfaceIpv6AddressCustomizer extends IpAddressReader
        implements InitializingListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceIpv6AddressCustomizer.class);

    public SubInterfaceIpv6AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                             @Nonnull final NamingContext interfaceContext) {
        super(interfaceContext, true, new DumpCacheManager.DumpCacheManagerBuilder<IpAddressDetailsReplyDump, IfaceDumpFilter>()
                .withExecutor(createAddressDumpExecutor(futureJVppCore))
                .withCacheKeyFactory(subInterfaceScopedCacheKeyFactory(IpAddressDetailsReplyDump.class))
                .build());
    }

    private static String getSubInterfaceName(@Nonnull final InstanceIdentifier<Address> id) {
        return SubInterfaceUtils.getSubInterfaceName(id.firstKeyOf(Interface.class).getName(),
                Math.toIntExact(id.firstKeyOf(SubInterface.class).getIdentifier()));
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
        LOG.debug("Reading attributes for sub-interface address: {}", id);
        final Optional<IpAddressDetails> ipAddressDetails =
                findIpv6AddressDetailsByIp(subInterfaceAddressDumpSupplier(id, ctx), id.firstKeyOf(Address.class).getIp());

        if (ipAddressDetails.isPresent()) {
            final IpAddressDetails detail = ipAddressDetails.get();
            builder.setIp(arrayToIpv6AddressNoZone(detail.ip));
            builder.setPrefixLength((short) Byte.toUnsignedInt(detail.prefixLength));

            if (LOG.isDebugEnabled()) {
                final String subInterfaceName = getSubInterfaceName(id);
                final int subInterfaceIndex = getInterfaceContext().getIndex(subInterfaceName, ctx.getMappingContext());
                LOG.debug("Attributes for {} sub-interface (id={}) address {} successfully read: {}",
                        subInterfaceName, subInterfaceIndex, id, builder.build());
            }
        }
    }

    @Override
    @Nonnull
    public List<AddressKey> getAllIds(@Nonnull InstanceIdentifier<Address> id, @Nonnull ReadContext ctx)
            throws ReadFailedException {
        LOG.debug("Reading list of keys for sub-interface addresses: {}", id);
        return getAllIpv6AddressIds(subInterfaceAddressDumpSupplier(id, ctx), AddressKey::new);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Address> readData) {
        ((Ipv6Builder) builder).setAddress(readData);
    }

    @Override
    @Nonnull
    public Initialized<Address> init(
            @Nonnull final InstanceIdentifier<Address> id, @Nonnull final Address readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id), readValue);
    }

    private InstanceIdentifier<Address> getCfgId(final InstanceIdentifier<Address> id) {
        return SubInterfaceCustomizer.getCfgId(RWUtils.cutId(id, SubInterface.class))
                .child(Ipv6.class)
                .child(Address.class, new AddressKey(id.firstKeyOf(Address.class)));
    }
}
