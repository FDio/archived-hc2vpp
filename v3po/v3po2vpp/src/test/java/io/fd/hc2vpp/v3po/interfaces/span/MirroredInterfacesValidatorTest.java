/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.v3po.interfaces.span;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.SpanState;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Span;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.span.attributes.mirrored.interfaces.MirroredInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.span.attributes.mirrored.interfaces.MirroredInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MirroredInterfacesValidatorTest {

    private InstanceIdentifier<MirroredInterface> validId;
    private MirroredInterfaceValidator validator;
    private static final String IFACE_NAME = "iface";
    private static final String SRC_IFACE_NAME = "src-iface";

    @Mock
    private WriteContext writeContext;

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new MirroredInterfaceValidator(ifcContext, id -> id.firstKeyOf(Interface.class).getName());
        validId = InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                .augmentation(VppInterfaceAugmentation.class).child(Span.class)
                .child(MirroredInterfaces.class)
                .child(MirroredInterface.class);
    }

    @Test
    public void testWriteSuccessful()
            throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(validId, validData(), writeContext);
    }

    @Test
    public void testWriteFailed() {
        validateWritingIncorrectData(incorrectDataNoIfaceRef());
        validateWritingIncorrectData(incorrectDataNoSpanState());
    }

    @Test
    public void testUpdateSuccessful()
            throws DataValidationFailedException.UpdateValidationFailedException {
        validator.validateUpdate(validId, validData(), validData(), writeContext);
    }

    @Test
    public void testDeleteSuccessful()
            throws DataValidationFailedException.DeleteValidationFailedException {
        validator.validateDelete(validId, validData(), writeContext);
    }

    private void validateWritingIncorrectData(final MirroredInterface data) {
        try {
            validator.validateWrite(validId, data, writeContext);
        } catch (DataValidationFailedException.CreateValidationFailedException e) {
            assertTrue(e instanceof DataValidationFailedException.CreateValidationFailedException);
            return;
        }
        fail("Verifying of invalid combination failed");
    }

    private MirroredInterface incorrectDataNoIfaceRef() {
        return new MirroredInterfaceBuilder()
                .setIfaceRef(null)
                .setState(SpanState.Receive)
                .build();
    }

    private MirroredInterface incorrectDataNoSpanState() {
        return new MirroredInterfaceBuilder()
                .setIfaceRef(SRC_IFACE_NAME)
                .setState(null)
                .build();
    }

    private MirroredInterface validData() {
        return new MirroredInterfaceBuilder()
                .setIfaceRef(SRC_IFACE_NAME)
                .setState(SpanState.Receive)
                .build();
    }
}
