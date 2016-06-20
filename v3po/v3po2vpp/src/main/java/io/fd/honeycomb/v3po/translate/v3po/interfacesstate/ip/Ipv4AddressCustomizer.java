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
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
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
 * Customizer for read operations for {@link Address} of {@link Ipv4}
 */
public class Ipv4AddressCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Address, AddressKey, AddressBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4AddressCustomizer.class);

    private static final String CACHE_KEY = Ipv4AddressCustomizer.class.getName();

    public Ipv4AddressCustomizer(FutureJVpp futureJvpp) {
        super(futureJvpp);
    }

    @Override
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
            byte[] identifingIpBytes = TranslateUtils.ipv4AddressNoZoneToArray(key.getIp());

            IpAddressDetails detail = details.stream()
                    .filter(singleDetail -> Arrays.equals(identifingIpBytes, singleDetail.ip))
                    .collect(RWUtils.singleItemCollector());

            builder.setIp(TranslateUtils.arrayToIpv4AddressNoZone(detail.ip))
                    .setSubnet(new PrefixLengthBuilder()
                            .setPrefixLength(Short.valueOf(detail.prefixLength)).build());
            LOG.info("Address read successfull");

        } else {
            LOG.warn("No address dump present");
        }
    }

    @Override
    public List<AddressKey> getAllIds(@Nonnull InstanceIdentifier<Address> id, @Nonnull ReadContext context)
            throws ReadFailedException {
        LOG.debug("Extracting keys..");

        Optional<IpAddressDetailsReplyDump> dumpOptional = dumpAddresses(id, context);

        if (dumpOptional.isPresent() && dumpOptional.get().ipAddressDetails != null) {

            List<IpAddressDetails> details = dumpOptional.get().ipAddressDetails;

            return details.stream()
                    .map(detail -> new AddressKey(TranslateUtils.arrayToIpv4AddressNoZone(detail.ip)))
                    .collect(Collectors.toList());
        } else {
            LOG.warn("No dump present");
            return Collections.emptyList();
        }
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Address> readData) {
        ((Ipv4Builder) builder).setAddress(readData);
    }

    // TODO refactor after there is an more generic implementation of cache
    // operations
    private Optional<IpAddressDetailsReplyDump> dumpAddresses(InstanceIdentifier<Address> id, ReadContext ctx)
            throws ReadFailedException {
        Optional<IpAddressDetailsReplyDump> dumpFromCache = dumpAddressFromCache(ctx.getModificationCache());

        if (dumpFromCache.isPresent()) {
            return dumpFromCache;
        }

        Optional<IpAddressDetailsReplyDump> dumpFromOperational;
        try {
            dumpFromOperational = dumpAddressFromOperationalData();
        } catch (VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }

        if (dumpFromOperational.isPresent()) {
            ctx.getModificationCache().put(CACHE_KEY, dumpFromOperational.get());
        }

        return dumpFromOperational;
    }

    private Optional<IpAddressDetailsReplyDump> dumpAddressFromCache(ModificationCache cache) {
        LOG.debug("Dumping from cache...");
        return Optional.fromNullable((IpAddressDetailsReplyDump) cache.get(CACHE_KEY));
    }

    private Optional<IpAddressDetailsReplyDump> dumpAddressFromOperationalData() throws VppBaseCallException {
        LOG.debug("Dumping from operational data...");
        return Optional.fromNullable(
                TranslateUtils.getReply(getFutureJVpp().ipAddressDump(new IpAddressDump()).toCompletableFuture()));
    }

}
