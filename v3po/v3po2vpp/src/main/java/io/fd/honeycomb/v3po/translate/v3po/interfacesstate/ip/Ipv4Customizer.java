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

import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.spi.read.ChildReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.ipv4.AddressBuilder;
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

public class Ipv4Customizer extends FutureJVppCustomizer implements ChildReaderCustomizer<Ipv4, Ipv4Builder> {

    private static final Logger LOG = LoggerFactory.getLogger(Ipv4Customizer.class);

    private final NamingContext interfaceContext;

    public Ipv4Customizer(@Nonnull final FutureJVpp futureJvpp, final NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Ipv4 readValue) {
        ((Interface2Builder) parentBuilder).setIpv4(readValue);
    }

    @Nonnull
    @Override
    public Ipv4Builder getBuilder(@Nonnull final InstanceIdentifier<Ipv4> id) {
        return new Ipv4Builder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Ipv4> id, @Nonnull final Ipv4Builder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for IPv4: {}", id);
        final IpAddressDump dumpRequest = new IpAddressDump();
        dumpRequest.isIpv6 = 0;
        dumpRequest.swIfIndex = interfaceContext.getIndex(id.firstKeyOf(Interface.class).getName(), ctx.getMappingContext());
        final CompletionStage<IpAddressDetailsReplyDump> addressDumpFuture = getFutureJVpp().ipAddressDump(dumpRequest);

        // TODO consider extracting customizer for address
        final IpAddressDetailsReplyDump reply;
        try {
            reply = TranslateUtils.getReply(addressDumpFuture.toCompletableFuture());
        } catch (VppBaseCallException e) {
            LOG.warn("Unable to read IPv4 attributes for {}", id, e);
            throw new ReadFailedException(id, e);
        }
        if(reply != null && reply.ipAddressDetails != null) {
            builder.setAddress(
                reply.ipAddressDetails.stream()
                    .map(Ipv4Customizer::addressDetailsToIpv4)
                    .collect(Collectors.toList()));
        }

        // TODO what about other children ?
    }

    private static Address addressDetailsToIpv4(IpAddressDetails details) {
        return new AddressBuilder()
            .setIp(TranslateUtils.arrayToIpv4AddressNoZone(details.ip))
            .setSubnet(new PrefixLengthBuilder().setPrefixLength((short) details.prefixLength).build())
//            .setOrigin()
            .build();
    }
}
