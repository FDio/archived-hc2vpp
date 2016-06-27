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

package io.fd.honeycomb.v3po.translate.v3po.interfacesstate.ip;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.ReadTimeoutException;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.address.subnet.PrefixLengthBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.IpAddressDetails;
import org.openvpp.jvpp.dto.IpAddressDetailsReplyDump;
import org.openvpp.jvpp.dto.IpAddressDump;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for read operations for {@link Address} of {@link Ipv4}.
 */
public class Ipv4AddressCustomizer extends FutureJVppCustomizer
    implements ListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4AddressCustomizer.class);

    private static final String CACHE_KEY = Ipv4AddressCustomizer.class.getName();

    private final NamingContext interfaceContext;

    public Ipv4AddressCustomizer(@Nonnull final FutureJVpp futureJvpp, @Nonnull final NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = interfaceContext;
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
        LOG.debug("Reading attributes...");

        Optional<IpAddressDetailsReplyDump> dumpOptional = dumpAddresses(id, ctx);

        if (dumpOptional.isPresent() && dumpOptional.get().ipAddressDetails != null) {
            List<IpAddressDetails> details = dumpOptional.get().ipAddressDetails;

            AddressKey key = id.firstKeyOf(Address.class);

            IpAddressDetails detail = details.stream()
                .filter(singleDetail -> key.getIp().equals(TranslateUtils.arrayToIpv4AddressNoZone(singleDetail.ip)))
                .collect(RWUtils.singleItemCollector());

            builder.setIp(TranslateUtils.arrayToIpv4AddressNoZone(detail.ip))
                .setSubnet(new PrefixLengthBuilder()
                    .setPrefixLength(Short.valueOf(detail.prefixLength)).build());
            LOG.info("Address read successful");
        } else {
            LOG.warn("No address dump present");
        }
    }

    @Override
    public List<AddressKey> getAllIds(@Nonnull InstanceIdentifier<Address> id, @Nonnull ReadContext context)
        throws ReadFailedException {
        // FIXME: this kind of logs provide very little information. At least the ID should be included so we know
        // from the logs what exact data is being processed
        // + Logs should be consistent in using punctuation
        LOG.debug("Extracting keys..");

        Optional<IpAddressDetailsReplyDump> dumpOptional = dumpAddresses(id, context);

        if (dumpOptional.isPresent() && dumpOptional.get().ipAddressDetails != null) {

            return dumpOptional.get().ipAddressDetails.stream()
                .map(detail -> new AddressKey(TranslateUtils.arrayToIpv4AddressNoZone(detail.ip)))
                .collect(Collectors.toList());
        } else {
            // FIXME if this is expected then WARN should not be emitted
            // FIXME if this is not expected, throw an exception instead
            // Same in readCurrentAttributes()
            LOG.warn("No dump present");
            return Collections.emptyList();
        }
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Address> readData) {
        ((Ipv4Builder) builder).setAddress(readData);
    }

    // TODO refactor after there is a more generic implementation of cache operations
    // FIXME update TODO with what exactly should be refactored and how
    // TODO refactor after there is an more generic implementation of cache
    // operations
    private Optional<IpAddressDetailsReplyDump> dumpAddresses(InstanceIdentifier<Address> id, ReadContext ctx)
            throws ReadFailedException {
        final String cacheKey = CACHE_KEY + id.firstKeyOf(Interface.class).getName();
        Optional<IpAddressDetailsReplyDump> dumpFromCache = dumpAddressFromCache(cacheKey, ctx.getModificationCache());

        if (dumpFromCache.isPresent()) {
            return dumpFromCache;
        }

        Optional<IpAddressDetailsReplyDump> dumpFromOperational;
        try {
            dumpFromOperational = dumpAddressFromOperationalData(id, ctx.getMappingContext());
        } catch (VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }

        if (dumpFromOperational.isPresent()) {
            ctx.getModificationCache().put(cacheKey, dumpFromOperational.get());
        }

        return dumpFromOperational;
    }

    private Optional<IpAddressDetailsReplyDump> dumpAddressFromCache(final String cacheKey,
                                                                     ModificationCache cache) {
        LOG.debug("Dumping from cache...");
        return Optional.fromNullable((IpAddressDetailsReplyDump) cache.get(cacheKey));
    }

    private Optional<IpAddressDetailsReplyDump> dumpAddressFromOperationalData(final InstanceIdentifier<Address> id,
                                                                               final MappingContext mappingContext)
        throws VppBaseCallException, ReadTimeoutException {
        LOG.debug("Dumping from operational data...");
        final IpAddressDump dumpRequest = new IpAddressDump();
        dumpRequest.isIpv6 = 0;
        dumpRequest.swIfIndex = interfaceContext.getIndex(id.firstKeyOf(Interface.class).getName(), mappingContext);
        return Optional.fromNullable(
            TranslateUtils.getReplyForRead(getFutureJVpp().ipAddressDump(dumpRequest).toCompletableFuture(), id));
    }

}
