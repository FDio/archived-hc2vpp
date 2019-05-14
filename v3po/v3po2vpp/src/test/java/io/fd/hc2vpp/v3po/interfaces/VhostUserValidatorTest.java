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
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VhostUserRole;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VhostUserValidatorTest {

    private VhostUserValidator validator;

    @Mock
    private WriteContext writeContext;

    private static final String SOCKET = "testSocket";
    private static final String IFACE_NAME = "eth0";
    private static final InstanceIdentifier<VhostUser> ID =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(IFACE_NAME))
                    .augmentation(VppInterfaceAugmentation.class).child(VhostUser.class);

    @Before
    public void setUp() {
        initMocks(this);
        NamingContext ifcContext = new NamingContext("testInterfaceContext", "testInterfaceContext");
        validator = new VhostUserValidator(ifcContext);
    }

    @Test
    public void testWriteSuccessful() throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(ID, getVhostUser(SOCKET), writeContext);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteFailedNoSocket() throws DataValidationFailedException.CreateValidationFailedException {
        validator.validateWrite(ID, getVhostUser(null), writeContext);
    }

    @Test
    public void testUpdateSuccessful() throws DataValidationFailedException.UpdateValidationFailedException {
        validator.validateUpdate(ID, getVhostUser(SOCKET),
                getVhostUser(SOCKET), writeContext);
    }

    @Test
    public void testDeleteeSuccessful() throws DataValidationFailedException.DeleteValidationFailedException {
        validator.validateDelete(ID, getVhostUser(SOCKET), writeContext);
    }

    private VhostUser getVhostUser(final String socketName) {
        return VhostUserCustomizerTest.generateVhostUser(VhostUserRole.Client, socketName);
    }
}
