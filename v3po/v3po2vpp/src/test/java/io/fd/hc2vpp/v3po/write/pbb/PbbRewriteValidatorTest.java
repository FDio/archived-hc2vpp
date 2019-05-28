/*
 * Copyright (c) 2019 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.v3po.write.pbb;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.pbb.types.rev161214.Operation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.PbbRewriteInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.interfaces._interface.PbbRewriteBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PbbRewriteValidatorTest {

    private PbbRewriteValidator validator;
    private InstanceIdentifier<PbbRewrite> validId;
    private InstanceIdentifier<PbbRewrite> invalidId;

    @Mock
    private WriteContext writeContext;

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new PbbRewriteValidator(ifcContext);
        validId = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey("pbb-interface"))
                .augmentation(PbbRewriteInterfaceAugmentation.class)
                .child(PbbRewrite.class);

        invalidId = InstanceIdentifier.create(PbbRewrite.class);
    }

    @Test
    public void testWriteSuccessful()
            throws CreateValidationFailedException {
        validator.validateWrite(validId, validData(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedInvalidIID()
            throws CreateValidationFailedException {
        validator.validateWrite(invalidId, validData(), writeContext);
    }

    @Test
    public void testWriteFailedInvalidData() {
        verifyInvalidWriteDataCombination(invalidDataNoDestination());
        verifyInvalidWriteDataCombination(invalidDataNoSource());
        verifyInvalidWriteDataCombination(invalidDataNoItag());
        verifyInvalidWriteDataCombination(invalidDataNoOperation());
        verifyInvalidWriteDataCombination(invalidDataNoVlan());
    }

    @Test
    public void testUpdateFailedInvalidData() {
        verifyInvalidUpdateDataCombination(invalidDataNoDestination());
        verifyInvalidUpdateDataCombination(invalidDataNoSource());
        verifyInvalidUpdateDataCombination(invalidDataNoItag());
        verifyInvalidUpdateDataCombination(invalidDataNoOperation());
        verifyInvalidUpdateDataCombination(invalidDataNoVlan());
    }

    @Test
    public void testDeleteFailedInvalidData() {
        verifyInvalidDeleteDataCombination(invalidDataNoDestination());
        verifyInvalidDeleteDataCombination(invalidDataNoSource());
        verifyInvalidDeleteDataCombination(invalidDataNoItag());
        verifyInvalidDeleteDataCombination(invalidDataNoOperation());
        verifyInvalidDeleteDataCombination(invalidDataNoVlan());
    }

    private void verifyInvalidWriteDataCombination(final PbbRewrite invalidData) {
        try {
            validator.validateWrite(validId, invalidData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof CreateValidationFailedException);
            return;
        }

        fail("Verifying of invalid combination failed");
    }

    private void verifyInvalidUpdateDataCombination(final PbbRewrite invalidData) {
        try {
            validator.validateUpdate(validId, validData(), invalidData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof UpdateValidationFailedException);
            return;
        }

        fail("Verifying of invalid combination failed");
    }

    private void verifyInvalidDeleteDataCombination(final PbbRewrite invalidData) {
        try {
            validator.validateDelete(validId, invalidData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof DeleteValidationFailedException);
            return;
        }

        fail("Verifying of invalid combination failed");
    }

    static PbbRewrite invalidDataNoDestination() {
        return new PbbRewriteBuilder()
                .setBVlanTagVlanId(1234)
                .setITagIsid(2L)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    static PbbRewrite invalidDataNoSource() {
        return new PbbRewriteBuilder()
                .setBVlanTagVlanId(1234)
                .setITagIsid(2L)
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    static PbbRewrite invalidDataNoItag() {
        return new PbbRewriteBuilder()
                .setBVlanTagVlanId(1234)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    static PbbRewrite invalidDataNoVlan() {
        return new PbbRewriteBuilder()
                .setITagIsid(2L)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    static PbbRewrite invalidDataNoOperation() {
        return new PbbRewriteBuilder()
                .setITagIsid(2L)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }

    static PbbRewrite validData() {
        return new PbbRewriteBuilder()
                .setBVlanTagVlanId(1234)
                .setITagIsid(2L)
                .setSourceAddress(new MacAddress("aa:aa:aa:aa:aa:aa"))
                .setDestinationAddress(new MacAddress("bb:bb:bb:bb:bb:bb"))
                .setInterfaceOperation(Operation.Pop2)
                .build();
    }
}
