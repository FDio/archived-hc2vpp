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


import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.ioampot.dto.PotProfileShowConfigDetails;
import io.fd.jvpp.ioampot.dto.PotProfileShowConfigDetailsReplyDump;
import io.fd.jvpp.ioampot.dto.PotProfileShowConfigDump;
import io.fd.jvpp.ioampot.future.FutureJVppIoampot;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.PotProfiles;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.ProfileIndexRange;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileList;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileListBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profile.PotProfileListKey;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSet;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSetBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.ioam.sb.pot.rev170112.pot.profiles.PotProfileSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PotProfileReaderCustomizerTest extends ListReaderCustomizerTest<PotProfileList,PotProfileListKey,
        PotProfileListBuilder>{

    @Mock
    FutureJVppIoampot jVppIoamPot;

    public PotProfileReaderCustomizerTest() {
        super(PotProfileList.class, PotProfileSetBuilder.class);
    }

    @Override
    protected ReaderCustomizer<PotProfileList, PotProfileListBuilder> initCustomizer() {
        return new PotProfileReaderCustomizer(jVppIoamPot);
    }

    @Override
    public void setUp() {
        final PotProfileShowConfigDetailsReplyDump replyDump = new PotProfileShowConfigDetailsReplyDump();
        final PotProfileShowConfigDetails replyDetails = new PotProfileShowConfigDetails();
        replyDetails.bitMask = (long)0xF;
        replyDetails.id=0;
        replyDetails.lpc=1234;
        replyDetails.polynomialPublic=1234;
        replyDetails.prime=7;
        replyDetails.secretKey=1234;
        replyDetails.secretShare = 1234;
        replyDetails.validator = 1;
        replyDump.potProfileShowConfigDetails = Lists.newArrayList(replyDetails);
        doReturn(future(replyDump)).when(jVppIoamPot).potProfileShowConfigDump(any(PotProfileShowConfigDump.class));
    }

    private InstanceIdentifier<PotProfileList> getPotProfileListId(int id){
        return InstanceIdentifier.create(PotProfiles.class)
                .child(PotProfileSet.class, new PotProfileSetKey("potprofile"))
                .child(PotProfileList.class, new PotProfileListKey(new ProfileIndexRange(id)));
    }

    @Test
    public void testReadCurrentAttributes() throws ReadFailedException {
        PotProfileListBuilder builder = new PotProfileListBuilder();
        getCustomizer().readCurrentAttributes(getPotProfileListId(0),builder,ctx);
        assertEquals(0x4,builder.getNumberOfBits().longValue());
        assertEquals(0,builder.getIndex().getValue().intValue());
        assertEquals(1234,builder.getLpc().longValue());
        assertEquals(1234,builder.getPublicPolynomial().longValue());
        assertEquals(7,builder.getPrimeNumber().longValue());
        assertEquals(1234,builder.getValidatorKey().longValue());
        assertEquals(1234,builder.getSecretShare().longValue());
        assertEquals(true,builder.isValidator());

    }
}
