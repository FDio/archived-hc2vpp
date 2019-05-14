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

package io.fd.hc2vpp.v3po.interfaces;

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.DataValidationFailedException.UpdateValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.Ethernet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.EthernetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EthernetValidatorTest {

    private EthernetValidator validator;

    @Mock
    private WriteContext writeContext;

    private static final String IF_NAME = "eth0";
    private static final int IF_MTU = 1234;
    private static final InstanceIdentifier<Ethernet> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IF_NAME)).augmentation(
                    VppInterfaceAugmentation.class).child(Ethernet.class);

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new EthernetValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful() throws CreateValidationFailedException {
        validator.validateWrite(ID, ethernet(IF_MTU), writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteFailedNoMTU() throws CreateValidationFailedException {
        validator.validateWrite(ID, ethernet(null), writeContext);
    }

    @Test
    public void testUpdateSuccessful() throws UpdateValidationFailedException {
        validator.validateUpdate(ID, ethernet(IF_MTU), ethernet(IF_MTU + 1), writeContext);
    }

    static Ethernet ethernet(final Integer mtu) {
        final EthernetBuilder ethernet = new EthernetBuilder();
        ethernet.setMtu(mtu);
        return ethernet.build();
    }
}
