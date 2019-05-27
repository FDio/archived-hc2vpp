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

package io.fd.hc2vpp.v3po.write;

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.DeleteValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.InterfaceUnnumberedAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.UnnumberedBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceUnnumberedValidatorTest {

    private InterfaceUnnumberedValidator validator;

    @Mock
    private WriteContext writeContext;

    private static final String IF_NAME = "eth2";
    private static final String TARGET_IFC0_NAME = "eth0";
    private static final InstanceIdentifier<Unnumbered> ID = InstanceIdentifier.create(Interfaces.class)
            .child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(InterfaceUnnumberedAugmentation.class)
            .child(Unnumbered.class);

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new InterfaceUnnumberedValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful() throws CreateValidationFailedException {
        validator.validateWrite(ID, getUnnumberedIfc(TARGET_IFC0_NAME), writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteFailedNoUse() throws CreateValidationFailedException {
        validator.validateWrite(ID, getUnnumberedIfc(null), writeContext);
    }

    @Test
    public void testUpdateSuccessful() throws UpdateValidationFailedException {
        validator.validateUpdate(ID, getUnnumberedIfc(TARGET_IFC0_NAME), getUnnumberedIfc(TARGET_IFC0_NAME),
                writeContext);
    }

    @Test
    public void testDeleteeSuccessful() throws DeleteValidationFailedException {
        validator.validateDelete(ID, getUnnumberedIfc(TARGET_IFC0_NAME), writeContext);
    }

    private Unnumbered getUnnumberedIfc(String use) {
        return new UnnumberedBuilder().setUse(use).build();
    }
}
