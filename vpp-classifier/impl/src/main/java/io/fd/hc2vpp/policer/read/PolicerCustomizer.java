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

package io.fd.hc2vpp.policer.read;

import java.util.Optional;
import com.google.common.primitives.Longs;
import com.google.common.primitives.UnsignedInts;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.core.dto.PolicerDetails;
import io.fd.jvpp.core.dto.PolicerDetailsReplyDump;
import io.fd.jvpp.core.dto.PolicerDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.DscpType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionDrop;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionMarkDscp;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionTransmit;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterActionType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.MeterType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.PolicerRateType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.PolicerRoundType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.Policers;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.PolicersStateBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.VppDscpType;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ConformAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ConformActionBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ExceedAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ExceedActionBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ViolateAction;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policer.base.attributes.ViolateActionBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.state.Policer;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.state.PolicerBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.state.PolicerKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Dscp;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

final class PolicerCustomizer extends FutureJVppCustomizer
    implements InitializingListReaderCustomizer<Policer, PolicerKey, PolicerBuilder>,
    JvppReplyConsumer, ByteDataTranslator {

    private final DumpCacheManager<PolicerDetailsReplyDump, Void> dumpManager;

    PolicerCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
        dumpManager = new DumpCacheManager.DumpCacheManagerBuilder<PolicerDetailsReplyDump, Void>()
            .withExecutor(
                (id, param) -> getReplyForRead(getFutureJVpp().policerDump(new PolicerDump()).toCompletableFuture(),
                    id))
            .acceptOnly(PolicerDetailsReplyDump.class)
            .build();
    }

    @Nonnull
    @Override
    public List<PolicerKey> getAllIds(@Nonnull final InstanceIdentifier<Policer> id,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final Optional<PolicerDetailsReplyDump> dump = dumpManager.getDump(id, ctx.getModificationCache());

        if (!dump.isPresent() || dump.get().policerDetails.isEmpty()) {
            return Collections.emptyList();
        }
        return dump.get().policerDetails.stream().map(detail -> new PolicerKey(toString(detail.name)))
            .collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Policer> list) {
        ((PolicersStateBuilder) builder).setPolicer(list);
    }

    @Nonnull
    @Override
    public PolicerBuilder getBuilder(@Nonnull final InstanceIdentifier<Policer> id) {
        return new PolicerBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Policer> id,
                                      @Nonnull final PolicerBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final Optional<PolicerDetailsReplyDump> dump = dumpManager.getDump(id, ctx.getModificationCache());

        if (!dump.isPresent() || dump.get().policerDetails.isEmpty()) {
            return;
        }
        final PolicerKey key = id.firstKeyOf(Policer.class);
        final java.util.Optional<PolicerDetails> result =
            dump.get().policerDetails.stream().filter(detail -> key.equals(new PolicerKey(toString(detail.name)))).findFirst();
        if (!result.isPresent()) {
            return;
        }
        final PolicerDetails details = result.get();
        builder.setName(toString(details.name));
        builder.setCir(UnsignedInts.toLong(details.cir));
        builder.setEir(UnsignedInts.toLong(details.eir));
        builder.setCb(toUnsignedBigInteger(details.cb));
        builder.setEb(toUnsignedBigInteger(details.eb));
        builder.setRateType(PolicerRateType.forValue(details.rateType));
        builder.setRoundType(PolicerRoundType.forValue(details.roundType));
        builder.setType(MeterType.forValue(details.type));
        builder.setColorAware(byteToBoolean(details.colorAware));
        builder.setConformAction(parseConformAction(details));
        builder.setExceedAction(parseExceedAction(details));
        builder.setViolateAction(parseViolateAction(details));

        // operational only data:
        builder.setSingleRate(byteToBoolean(details.singleRate));
        builder.setScale(UnsignedInts.toLong(details.scale));
        builder.setCirTokensPerPeriod(UnsignedInts.toLong(details.cirTokensPerPeriod));
        builder.setPirTokensPerPeriod(UnsignedInts.toLong(details.pirTokensPerPeriod));
        builder.setCurrentLimit(UnsignedInts.toLong(details.currentLimit));
        builder.setCurrentBucket(UnsignedInts.toLong(details.currentBucket));
        builder.setExtendedLimit(UnsignedInts.toLong(details.extendedLimit));
        builder.setExtendedBucket(UnsignedInts.toLong(details.extendedBucket));
        builder.setLastUpdateTime(toUnsignedBigInteger(details.lastUpdateTime));
    }

    private BigInteger toUnsignedBigInteger(final long value) {
        return new BigInteger(1, Longs.toByteArray(value));
    }

    private Class<? extends MeterActionType> parseMeterActionType(final byte actionType) {
        switch (actionType) {
            case 0:
                return MeterActionDrop.class;
            case 1:
                return MeterActionTransmit.class;
            case 2:
                return MeterActionMarkDscp.class;
            default:
                throw new IllegalArgumentException("Unsupported meter action type " + actionType);
        }
    }

    private DscpType parseDscp(final byte dscp, final byte conformActionType) {
        if (dscp == -1 || conformActionType != 2) {
            return null;
        }
        VppDscpType vppDcspType = VppDscpType.forValue(dscp);
        if (vppDcspType != null) {
            return new DscpType(vppDcspType);
        }
        return new DscpType(new Dscp((short) dscp));
    }

    private ConformAction parseConformAction(final PolicerDetails details) {
        ConformActionBuilder action = new ConformActionBuilder();
        action.setMeterActionType(parseMeterActionType(details.conformActionType));
        action.setDscp(parseDscp(details.conformDscp, details.conformActionType));
        return action.build();
    }


    private ExceedAction parseExceedAction(final PolicerDetails details) {
        ExceedActionBuilder action = new ExceedActionBuilder();
        action.setMeterActionType(parseMeterActionType(details.exceedActionType));
        action.setDscp(parseDscp(details.exceedDscp, details.conformActionType));
        return action.build();
    }

    private ViolateAction parseViolateAction(final PolicerDetails details) {
        ViolateActionBuilder action = new ViolateActionBuilder();
        action.setMeterActionType(parseMeterActionType(details.violateActionType));
        action.setDscp(parseDscp(details.violateDscp, details.conformActionType));
        return action.build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Policer> id,
                                                  @Nonnull final Policer policer,
                                                  @Nonnull final ReadContext readContext) {
        return Initialized.create(getCfgId(id),
            new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.PolicerBuilder(
                policer)
                .setName(policer.getName())
                .build());
    }

    private static InstanceIdentifier<org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.Policer> getCfgId(
        final InstanceIdentifier<Policer> id) {
        final PolicerKey key = id.firstKeyOf(Policer.class);
        return InstanceIdentifier.create(Policers.class).child(
            org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.Policer.class,
            new org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.policer.rev170315.policers.PolicerKey(
                key.getName()));
    }
}
