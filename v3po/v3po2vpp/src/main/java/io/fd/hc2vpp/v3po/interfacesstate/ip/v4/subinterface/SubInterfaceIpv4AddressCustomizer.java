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

package io.fd.hc2vpp.v3po.interfacesstate.ip.v4.subinterface;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.SubInterfaceCustomizer;
import io.fd.hc2vpp.v3po.interfacesstate.ip.dump.params.IfaceDumpFilter;
import io.fd.hc2vpp.v3po.interfacesstate.ip.readers.IpAddressReader;
import io.fd.hc2vpp.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.vpp.jvpp.core.dto.IpAddressDetails;
import io.fd.vpp.jvpp.core.dto.IpAddressDetailsReplyDump;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev161214.sub._interface.ip4.attributes.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Read customizer for sub-interface Ipv4 addresses.
 */
public class SubInterfaceIpv4AddressCustomizer extends IpAddressReader
        implements InitializingListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceIpv4AddressCustomizer.class);

    public SubInterfaceIpv4AddressCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                             @Nonnull final NamingContext interfaceContext) {
        super(interfaceContext, false, new DumpCacheManager.DumpCacheManagerBuilder<IpAddressDetailsReplyDump, IfaceDumpFilter>()
                .withExecutor(createAddressDumpExecutor(futureJVppCore))
                //same as with ipv4 addresses for interfaces, these must have cache scope of their parent sub-interface
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
                findIpv4AddressDetailsByIp(subInterfaceAddressDumpSupplier(id, ctx), id.firstKeyOf(Address.class).getIp());

        if (ipAddressDetails.isPresent()) {
            final IpAddressDetails detail = ipAddressDetails.get();
            builder.setIp(arrayToIpv4AddressNoZone(detail.ip));
            builder.setSubnet(new PrefixLengthBuilder().setPrefixLength((short) detail.prefixLength).build());

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
        return getAllIpv4AddressIds(subInterfaceAddressDumpSupplier(id, ctx), AddressKey::new);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Address> readData) {
        ((Ipv4Builder) builder).setAddress(readData);
    }

    @Override
    public Initialized<Address> init(
            @Nonnull final InstanceIdentifier<Address> id, @Nonnull final Address readValue,
            @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id), readValue);
    }

    private InstanceIdentifier<Address> getCfgId(final InstanceIdentifier<Address> id) {
        return SubInterfaceCustomizer.getCfgId(RWUtils.cutId(id, SubInterface.class))
                .child(Ipv4.class)
                .child(Address.class, new AddressKey(id.firstKeyOf(Address.class)));
    }
}
