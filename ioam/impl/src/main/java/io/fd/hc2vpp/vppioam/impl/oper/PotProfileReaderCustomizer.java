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

package io.fd.hc2vpp.vppioam.impl.oper;

import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.vppioam.impl.util.FutureJVppIoampotCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.jvpp.ioampot.dto.PotProfileShowConfigDetails;
import io.fd.jvpp.ioampot.dto.PotProfileShowConfigDetailsReplyDump;
import io.fd.jvpp.ioampot.dto.PotProfileShowConfigDump;
import io.fd.jvpp.ioampot.future.FutureJVppIoampot;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.ProfileIndexRange;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileList;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileListBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileListKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSetBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PotProfileReaderCustomizer extends FutureJVppIoampotCustomizer implements JvppReplyConsumer,
        InitializingListReaderCustomizer<PotProfileList,PotProfileListKey,PotProfileListBuilder> {

    private static final Logger LOG = LoggerFactory.getLogger(PotProfileReaderCustomizer.class);

    public PotProfileReaderCustomizer(FutureJVppIoampot futureJVppIoamPot) {
        super(futureJVppIoamPot);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<PotProfileList> instanceIdentifier,
                                                  @Nonnull final PotProfileList potProfileList,
                                                  @Nonnull final ReadContext readContext) {
        return Initialized.create(instanceIdentifier,new PotProfileListBuilder(potProfileList).build());
    }

    @Nonnull
    @Override
    public List<PotProfileListKey> getAllIds(@Nonnull final InstanceIdentifier<PotProfileList> instanceIdentifier,
                                             @Nonnull final ReadContext readContext) throws ReadFailedException {

        //vpp will always return 2 entries with id's 0 and 1
        //will contain 0 values if not previously configured

        List<PotProfileListKey> allIds = new ArrayList<>(2);
        allIds.add(new PotProfileListKey(new ProfileIndexRange(0)));
        allIds.add(new PotProfileListKey(new ProfileIndexRange(1)));
        return allIds;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<PotProfileList> list) {
        ((PotProfileSetBuilder)builder).setPotProfileList(list);
    }

    @Nonnull
    @Override
    public PotProfileListBuilder getBuilder(@Nonnull final InstanceIdentifier<PotProfileList> instanceIdentifier) {
        return new PotProfileListBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<PotProfileList> instanceIdentifier,
                                      @Nonnull final PotProfileListBuilder builder,
                                      @Nonnull final ReadContext readContext) throws ReadFailedException {
        final PotProfileShowConfigDump request = new PotProfileShowConfigDump();
        PotProfileListKey key = instanceIdentifier.firstKeyOf(PotProfileList.class);
        request.id = key.getIndex().getValue().byteValue();
        final PotProfileShowConfigDetailsReplyDump reply = getReplyForRead(getFutureJVppIoampot()
                .potProfileShowConfigDump(request)
                .toCompletableFuture(), instanceIdentifier);

        if (reply == null || reply.potProfileShowConfigDetails == null || reply.potProfileShowConfigDetails.isEmpty()) {
            LOG.debug("Vpp returned no pot profiles");
            return;
        }

        final PotProfileShowConfigDetails details = reply.potProfileShowConfigDetails.get(0);

        builder.setValidator(details.validator == 1);
        builder.setValidatorKey(BigInteger.valueOf(details.secretKey));
        builder.setSecretShare(BigInteger.valueOf(details.secretShare));
        builder.setPrimeNumber(BigInteger.valueOf(details.prime));
        builder.setPublicPolynomial(BigInteger.valueOf(details.polynomialPublic));
        builder.setIndex(new ProfileIndexRange((int)details.id));
        builder.setLpc(BigInteger.valueOf(details.lpc));
        builder.setNumberOfBits(getMaxBitsfromBitmask(BigInteger.valueOf(details.bitMask)));

        LOG.info("Item {} successfully read: {}",instanceIdentifier, builder.build());
    }

    private static short getMaxBitsfromBitmask (BigInteger bitmask) {
        short numOfBits = 0;
        while ((bitmask.and(BigInteger.ONE)).equals(BigInteger.ONE)) {
            bitmask = bitmask.shiftRight(1);
            numOfBits++;
        }
        return numOfBits;
    }
}
