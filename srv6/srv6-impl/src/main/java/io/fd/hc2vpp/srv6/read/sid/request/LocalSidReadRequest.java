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

import static java.lang.Integer.parseInt;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.googlecode.ipv6.IPv6NetworkMask;
import io.fd.hc2vpp.srv6.read.ReadRequest;
import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.hc2vpp.srv6.util.LocatorContextManager;
import io.fd.hc2vpp.srv6.util.function.LocalSidFunctionReadBindingRegistry;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.SrLocalsidsDetails;
import io.fd.jvpp.core.dto.SrLocalsidsDetailsReplyDump;
import io.fd.jvpp.core.dto.SrLocalsidsDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6._static.rev180301.srv6._static.cfg.SidKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.Locator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.srv6.locators.locators.LocatorKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDT4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDT6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndDX6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6EndpointType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.Srv6FuncOpcodeUnreserved;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocalSidReadRequest extends JVppRequest implements ReadRequest<Sid, SidKey, SidBuilder> {

    private static final Map<Integer, Class<? extends Srv6EndpointType>> VPP_END_FUNC_REGISTER;
    private static final SrLocalsidsDump STATIC_DUMP_REQUEST = new SrLocalsidsDump();
    private static final SrLocalsidsDetailsReplyDump STATIC_EMPTY_REPLY = new SrLocalsidsDetailsReplyDump();

    static {
        VPP_END_FUNC_REGISTER = ImmutableMap.<Integer, Class<? extends Srv6EndpointType>>builder()
                .put(1, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.End.class)
                .put(2, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndX.class)
                .put(3, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.types.rev180301.EndT.class)
                .put(5, EndDX2.class)
                .put(6, EndDX6.class)
                .put(7, EndDX4.class)
                .put(8, EndDT6.class)
                .put(9, EndDT4.class)
                .build();
    }

    private final DumpCacheManager<SrLocalsidsDetailsReplyDump, Void> dumpManager;
    private final LocatorContextManager locatorContext;
    private final LocalSidFunctionReadBindingRegistry registry;

    public LocalSidReadRequest(final FutureJVppCore api,
                               final LocatorContextManager locatorContext,
                               final LocalSidFunctionReadBindingRegistry registry) {
        super(api);
        this.dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<SrLocalsidsDetailsReplyDump, Void>()
                .acceptOnly(SrLocalsidsDetailsReplyDump.class)
                .withExecutor((identifier, params) -> getReplyForRead(
                        api.srLocalsidsDump(STATIC_DUMP_REQUEST).toCompletableFuture(), identifier))
                .build();
        this.locatorContext = locatorContext;
        this.registry = registry;
    }

    /**
     * Extracts Operational code (SRv6 endpoint function) from provided SID value. SID value consists of two parts.
     * First part is Locator defined by its IPv6 address and length (stored in mappingContext referenced
     * by locator name). Second part is Operational code (endpoint function). Locator length(number of bits) divides SID
     * address to bits used for locator value and bits used for endpoint function.
     *
     * @see <a href="https://tools.ietf.org/html/draft-filsfils-spring-srv6-network-programming-04">
     *     SRv6 network programming (SRv6 Segment)</a>
     * @see <a href="https://tools.ietf.org/html/draft-raza-spring-srv6-yang-01">
     *      *     SRv6 Yang (SRv6 Types)</a>
     *
     * @param sid provided SRv6 SIDs IPv6 address
     * @param mappingContext mapping context which stores mapping for locator length
     * @param locName locator name used as a key to retrieve locator length from mapping context
     * @return operational code (endpoint function) of SRv6 SID address
     */
    private Srv6FuncOpcodeUnreserved extractOpCode(Ipv6Address sid, MappingContext mappingContext,
                                                   final String locName) {
        int locLength = LocatorContextManager.parseLength(locatorContext.getLocator(locName, mappingContext));
        com.googlecode.ipv6.IPv6Address ip = com.googlecode.ipv6.IPv6Address.fromString(sid.getValue());
        IPv6NetworkMask mask = IPv6NetworkMask.fromPrefixLength(locLength);
        com.googlecode.ipv6.IPv6Address locator = ip.maskWithNetworkMask(mask);

        long function = ip.toBigInteger().subtract(locator.toBigInteger()).longValue();

        return new Srv6FuncOpcodeUnreserved(function);
    }

    @Override
    @Nonnull
    public List<SidKey> readAllKeys(@Nonnull InstanceIdentifier<Sid> identifier, @Nonnull ReadContext ctx)
            throws ReadFailedException {
        final LocatorKey key = Preconditions.checkNotNull(identifier.firstKeyOf(Locator.class),
                "Identifier does not have %s ", LocatorKey.class);
        String locator = key.getName();

        return dumpManager.getDump(identifier, ctx.getModificationCache()).orElse(STATIC_EMPTY_REPLY).srLocalsidsDetails
                .stream()
                .filter(detail -> arrayToIpv6AddressNoZone(detail.addr.addr).getValue().contains(locator))
                .map(srLocalsidsDetails -> extractOpCode(arrayToIpv6AddressNoZone(srLocalsidsDetails.addr.addr),
                        ctx.getMappingContext(), locator))
                .map(SidKey::new)
                .collect(Collectors.toList());
    }

    @Override
    public void readSpecific(@Nonnull InstanceIdentifier<Sid> identifier, @Nonnull ReadContext ctx,
                             @Nonnull SidBuilder builder)
            throws ReadFailedException {
        final SidKey sidKey = Preconditions.checkNotNull(identifier.firstKeyOf(Sid.class),
                "Identifier does not contain %s ", SidKey.class);
        final LocatorKey locatorKey = Preconditions.checkNotNull(identifier.firstKeyOf(Locator.class),
                "Identifier does not contain %s ", Locator.class);

        // VPP stores SID address as whole without defining locator and function parts (or locator length).
        // It is necessary to split SID address to locator and function (operational code), because that is how SID
        // is identified in model. Currently we use locatorContext to store locator length, so it is possible to split
        // SID address back to locator (used as LocatorKey) and function (used as SidKey) or to construct SID from
        // from locator and function (opCode)
        Integer locLength = LocatorContextManager
                .parseLength(locatorContext.getLocator(locatorKey.getName(), ctx.getMappingContext()));
        Ipv6Address locator = LocatorContextManager
                .parseLocator(locatorContext.getLocator(locatorKey.getName(), ctx.getMappingContext()));

        Ipv6Address sidAddress =
                parseSrv6SidAddress(locator.getValue(), locLength.toString(), sidKey.getOpcode().getValue());

        dumpManager.getDump(identifier, ctx.getModificationCache()).orElse(STATIC_EMPTY_REPLY).srLocalsidsDetails
                .stream()
                .filter(detail -> Arrays.equals(detail.addr.addr, ipv6AddressNoZoneToArray(sidAddress)))
                .findFirst()
                .ifPresent(detail -> bindLocalSid(detail, ctx, locatorKey.getName(), sidAddress, builder));
    }

    private Ipv6Address parseSrv6SidAddress(final String locatorIp, final String locatorLength, final Long opcode) {
        com.googlecode.ipv6.IPv6Address ip =
                com.googlecode.ipv6.IPv6Address.fromString(locatorIp);
        IPv6NetworkMask mask = IPv6NetworkMask.fromPrefixLength(parseInt(locatorLength));
        com.googlecode.ipv6.IPv6Address srv6Sid = ip.maskWithNetworkMask(mask);
        return new Ipv6Address(srv6Sid.add(opcode.intValue()).toString());
    }

    private void bindLocalSid(final SrLocalsidsDetails detail, final ReadContext readContext, final String locName,
                              final Ipv6Address sidAddress, final SidBuilder builder) {
        Class<? extends Srv6EndpointType> behaviorType = parseEndBehaviorType(detail.behavior);
        Srv6FuncOpcodeUnreserved opcode = extractOpCode(sidAddress, readContext.getMappingContext(), locName);
        builder.setEndBehaviorType(behaviorType).withKey(new SidKey(opcode)).setOpcode(opcode);
        parseEndFunction(builder, detail, readContext);
    }

    private void parseEndFunction(SidBuilder builder, SrLocalsidsDetails detail, ReadContext readContext) {
        registry.bind(detail, readContext, builder);
    }

    private Class<? extends Srv6EndpointType> parseEndBehaviorType(short behavior) {
        return VPP_END_FUNC_REGISTER.get((int) behavior);
    }
}
