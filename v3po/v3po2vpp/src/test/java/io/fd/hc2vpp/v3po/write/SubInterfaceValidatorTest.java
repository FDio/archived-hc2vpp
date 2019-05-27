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
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.sub._interface.base.attributes.Match;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceValidatorTest {

    private SubInterfaceValidator validator;

    @Mock
    private WriteContext writeContext;

    private static final String SUPER_IF_NAME = "local0";
    private static final String SUB_IFACE_NAME = "local0.11";
    private static final long SUBIF_INDEX = 11;
    private static final InstanceIdentifier<SubInterface> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(SUPER_IF_NAME))
                    .augmentation(
                            SubinterfaceAugmentation.class).child(SubInterfaces.class)
                    .child(SubInterface.class, new SubInterfaceKey(SUBIF_INDEX));

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new SubInterfaceValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful() throws CreateValidationFailedException {
        validator
                .validateWrite(ID, generateSubInterface(11L, SubInterfaceCustomizerTest.generateMatch()), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoIdentifier() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateSubInterface(null, SubInterfaceCustomizerTest.generateMatch()),
                writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedNoMatch() throws CreateValidationFailedException {
        validator.validateWrite(ID, generateSubInterface(11L, null), writeContext);
    }

    private SubInterface generateSubInterface(final Long identifier, final Match match) {
        return new SubInterfaceBuilder()
                .setIdentifier(identifier)
                .setMatch(match)
                .setEnabled(true)
                .build();
    }
}
