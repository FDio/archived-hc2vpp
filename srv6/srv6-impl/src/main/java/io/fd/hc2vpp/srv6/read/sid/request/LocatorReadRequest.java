/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.read.sid.request;

import static io.fd.hc2vpp.srv6.Srv6Configuration.DEFAULT_LOCATOR_LENGTH;

import com.google.common.base.Preconditions;
import com.googlecode.ipv6.IPv6NetworkMask;
import io.fd.hc2vpp.srv6.read.ReadRequest;
import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.SrLocalsidsDetails;
import io.fd.jvpp.core.dto.SrLocalsidsDetailsReplyDump;
import io.fd.jvpp.core.dto.SrLocalsidsDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.Ipv6;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.fib.table.management.rev180521.VniReference;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ietf.srv6.base.rev180613.VppSrv6FibLocatorAugment;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ietf.srv6.base.rev180613.VppSrv6FibLocatorAugmentBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ietf.srv6.base.rev180613.vpp.srv6.fib.FibTableBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.locator.PrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6LocatorLen;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocatorReadRequest extends JVppRequest implements ReadRequest<Locator, LocatorKey, LocatorBuilder> {

    private static final SrLocalsidsDump STATIC_DUMP_REQUEST = new SrLocalsidsDump();
    private static final SrLocalsidsDetailsReplyDump STATIC_EMPTY_REPLY = new SrLocalsidsDetailsReplyDump();
    private final DumpCacheManager<SrLocalsidsDetailsReplyDump, Void> dumpManager;
    private final LocatorContextManager locatorCtx;

    public LocatorReadRequest(final FutureJVppCore api, LocatorContextManager locatorCtx) {
        super(api);
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<SrLocalsidsDetailsReplyDump, Void>()
                .acceptOnly(SrLocalsidsDetailsReplyDump.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        api.srLocalsidsDump(STATIC_DUMP_REQUEST).toCompletableFuture(), identifier))
                .build();
        this.locatorCtx = locatorCtx;
    }

    private Ipv6Address extractLocator(Ipv6Address sid, final MappingContext mappingContext, String locName) {
        /*
        * TODO(HC2VPP-353): VPP does not support locator length, therefore it is necessary to use default value for
        * locator length, if there is no other way of getting the value (e.g. hc2vpp starts with configuration already
        * present in VPP).
        * */
        int locLength = locName == null
                ? DEFAULT_LOCATOR_LENGTH
                : LocatorContextManager.parseLength(locatorCtx.getLocator(locName, mappingContext));
        com.googlecode.ipv6.IPv6Address ip = com.googlecode.ipv6.IPv6Address.fromString(sid.getValue());
        IPv6NetworkMask mask = IPv6NetworkMask.fromPrefixLength(locLength);
        // strip function part if present
        ip = ip.maskWithNetworkMask(mask);
        return new Ipv6AddressNoZone(ip.toString());
    }

    @Override
    @Nonnull
    public List<LocatorKey> readAllKeys(@Nonnull final InstanceIdentifier<Locator> identifier,
                                        @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        return dumpManager.getDump(identifier, ctx.getModificationCache()).orElse(STATIC_EMPTY_REPLY).srLocalsidsDetails
                .stream()
                .map(srLocalsidsDetails -> extractLocator(arrayToIpv6AddressNoZone(srLocalsidsDetails.addr.addr),
                        ctx.getMappingContext(), null).getValue())
                .map(LocatorKey::new)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void readSpecific(@Nonnull final InstanceIdentifier<Locator> identifier, @Nonnull final ReadContext ctx,
                             @Nonnull LocatorBuilder builder) throws ReadFailedException {
        final LocatorKey key = Preconditions.checkNotNull(identifier.firstKeyOf(Locator.class),
                "Identifier does not have %s ", LocatorKey.class);
        String locator = key.getName();

        dumpManager.getDump(identifier, ctx.getModificationCache()).orElse(STATIC_EMPTY_REPLY).srLocalsidsDetails
                .stream()
                .filter(detail -> arrayToIpv6AddressNoZone(detail.addr.addr).getValue().contains(locator))
                .findFirst()
                .ifPresent(srLocalsidsDetails -> bindLocalSid(srLocalsidsDetails, ctx.getMappingContext(), locator,
                        builder));
    }

    private void bindLocalSid(final SrLocalsidsDetails detail, final MappingContext mappingContext,
                              final String locName, LocatorBuilder builder) {
        Ipv6Address locator = extractLocator(arrayToIpv6AddressNoZone(detail.addr.addr), mappingContext, locName);
        int locLength = LocatorContextManager.parseLength(locatorCtx.getLocator(locName, mappingContext));

        builder.withKey(new LocatorKey(locator.getValue()))
                .setName(locator.getValue())
                .setPrefix(
                        new PrefixBuilder()
                                .setAddress(locator)
                                .setLength(new Srv6LocatorLen((short) locLength))
                                .build())
                .setIsDefault(false)
                .setEnable(true)
                .addAugmentation(VppSrv6FibLocatorAugment.class, new VppSrv6FibLocatorAugmentBuilder()
                        .setFibTable(new FibTableBuilder()
                                .setAddressFamily(Ipv6.class)
                                .setTableId(new VniReference(Integer.toUnsignedLong(detail.fibTable)))
                                .build())
                        .build());
    }
}
