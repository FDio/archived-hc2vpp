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

package io.fd.hc2vpp.vppioam.impl.config;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.ioampot.dto.PotProfileAdd;
import io.fd.vpp.jvpp.ioampot.dto.PotProfileAddReply;
import io.fd.vpp.jvpp.ioampot.dto.PotProfileDel;
import io.fd.vpp.jvpp.ioampot.dto.PotProfileDelReply;
import io.fd.vpp.jvpp.ioampot.future.FutureJVppIoampot;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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


public class IoamPotWriterCustomizerTest extends WriterCustomizerTest {

    @Mock
    protected FutureJVppIoampot jVppIoamPot;

    private IoamPotWriterCustomizer customizer;

    private static final String POT_TEST_NAME = "dummy";

    @Override
    public void setUpTest() throws Exception {
        customizer = new IoamPotWriterCustomizer(jVppIoamPot);
    }

    private static PotProfileList generatePotProfileList() {
        final PotProfileListBuilder builder= new PotProfileListBuilder();
        builder.setIndex(new ProfileIndexRange(1));
        builder.setNumberOfBits((short)56);
        builder.setKey(new PotProfileListKey(new ProfileIndexRange(1)));
        builder.setLpc(new BigInteger("1233"));
        builder.setPrimeNumber(new BigInteger("1001"));
        builder.setPublicPolynomial(new BigInteger("1234"));
        builder.setSecretShare(new BigInteger("1234"));
        builder.setValidator(true);
        builder.setValidatorKey(new BigInteger("1"));
        return builder.build();
    }

    private static PotProfileSet generatePotProfileSet(){
        ArrayList<PotProfileList> potProfiles = new ArrayList<>();
        potProfiles.add(generatePotProfileList());
        PotProfileSetBuilder builder = new PotProfileSetBuilder();
        builder.setActiveProfileIndex(new ProfileIndexRange(1));
        builder.setName(POT_TEST_NAME);
        builder.setPotProfileList(potProfiles);
        builder.setPathIdentifier("ACL");
        return builder.build();
    }

    private static InstanceIdentifier<PotProfileSet> getPotProfileSetId(String name) {
        return InstanceIdentifier.create(PotProfiles.class)
                .child(PotProfileSet.class, new PotProfileSetKey(name));
    }

    private void whenPotAddThenSuccess() {
        final PotProfileAddReply reply = new PotProfileAddReply();
        reply.context = 1;
        doReturn(future(reply)).when(jVppIoamPot).potProfileAdd(any(PotProfileAdd.class));
    }

    private void whenPotAddThenFailure() {
        doReturn(failedFuture()).when(jVppIoamPot).potProfileAdd(any(PotProfileAdd.class));
    }

    private void whenPotDelThenSuccess() {
        final PotProfileDelReply reply = new PotProfileDelReply();
        reply.context = 1;
        doReturn(future(reply)).when(jVppIoamPot).potProfileDel(any(PotProfileDel.class));
    }

    private void whenPotDelThenFailure() {
        doReturn(failedFuture()).when(jVppIoamPot).potProfileDel(any(PotProfileDel.class));
    }

    private static PotProfileAdd generatePotProfileAdd() {
        PotProfileAdd request = new PotProfileAdd();
        request.id = 1;
        request.validator = 1;
        request.secretKey = 1;
        request.secretShare = 1234;
        request.prime = 1001;
        request.maxBits = (short) 56;
        request.lpc = 1233;
        request.polynomialPublic = 1234;
        request.listNameLen = (byte)POT_TEST_NAME.getBytes(StandardCharsets.UTF_8).length;
        request.listName = POT_TEST_NAME.getBytes(StandardCharsets.UTF_8);

        return request;
    }

    private static PotProfileDel generatePotProfileDel(String name) {
        final PotProfileDel request = new PotProfileDel();
        request.listName = name.getBytes(StandardCharsets.UTF_8);
        request.listNameLen = (byte)name.getBytes(StandardCharsets.UTF_8).length;

        return request;
    }

    @Test
    public void testCreate() throws Exception {
        final PotProfileSet potProfileSet = generatePotProfileSet();
        final InstanceIdentifier<PotProfileSet> id = getPotProfileSetId(POT_TEST_NAME);

        whenPotAddThenSuccess();

        customizer.writeCurrentAttributes(id, potProfileSet, writeContext);

        verify(jVppIoamPot).potProfileAdd(generatePotProfileAdd());
    }

    @Test
    public void testCreateFailed() throws Exception {
        final PotProfileSet potProfileSet = generatePotProfileSet();
        final InstanceIdentifier<PotProfileSet> id = getPotProfileSetId(POT_TEST_NAME);

        whenPotAddThenFailure();

        try {
            customizer.writeCurrentAttributes(id, potProfileSet, writeContext);
        } catch (WriteFailedException e) {
            verify(jVppIoamPot).potProfileAdd(generatePotProfileAdd());

            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testDelete() throws Exception {
        final PotProfileSet potProfileSet = generatePotProfileSet();
        final InstanceIdentifier<PotProfileSet> id = getPotProfileSetId(POT_TEST_NAME);

        whenPotDelThenSuccess();

        customizer.deleteCurrentAttributes(id, potProfileSet, writeContext);

        verify(jVppIoamPot).potProfileDel(generatePotProfileDel(POT_TEST_NAME));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final PotProfileSet potProfileSet = generatePotProfileSet();
        final InstanceIdentifier<PotProfileSet> id = getPotProfileSetId(POT_TEST_NAME);

        whenPotDelThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, potProfileSet, writeContext);
        } catch (WriteFailedException e) {
            verify(jVppIoamPot).potProfileDel(generatePotProfileDel(POT_TEST_NAME));
            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");

        customizer.deleteCurrentAttributes(id, potProfileSet, writeContext);
    }
}
