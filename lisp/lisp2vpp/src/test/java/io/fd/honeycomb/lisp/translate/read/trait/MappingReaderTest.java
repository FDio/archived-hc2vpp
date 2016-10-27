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

package io.fd.honeycomb.lisp.translate.read.trait;


import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MappingReaderTest implements MappingReader {

    private InstanceIdentifier<LocalMapping> validVrfLocal;
    private InstanceIdentifier<LocalMapping> validBdLocal;
    private InstanceIdentifier<LocalMapping> invalidLocal;

    private InstanceIdentifier<RemoteMapping> validVrfRemote;
    private InstanceIdentifier<RemoteMapping> validBdRemote;
    private InstanceIdentifier<RemoteMapping> invalidRemote;


    @Before
    public void init() {
        validVrfLocal = InstanceIdentifier.create(VrfSubtable.class)
                .child(LocalMappings.class)
                .child(LocalMapping.class);

        validBdLocal = InstanceIdentifier.create(BridgeDomainSubtable.class)
                .child(LocalMappings.class)
                .child(LocalMapping.class);

        invalidLocal = InstanceIdentifier.create(LocalMapping.class);

        validVrfRemote = InstanceIdentifier.create(VrfSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class);

        validBdRemote = InstanceIdentifier.create(BridgeDomainSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class);

        invalidRemote = InstanceIdentifier.create(RemoteMapping.class);
    }

    @Test
    public void testVrfLocalValid() {
        assertEquals(VRF_MAPPINGS_ONLY, subtableFilterForLocalMappings(validVrfLocal));
    }

    @Test
    public void testBridgeDomainLocalValid() {
        assertEquals(BRIDGE_DOMAIN_MAPPINGS_ONLY, subtableFilterForLocalMappings(validBdLocal));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLocalInvalid() {
        subtableFilterForLocalMappings(invalidLocal);
    }

    @Test
    public void testVrfRemoteValid() {
        assertEquals(VRF_MAPPINGS_ONLY, subtableFilterForRemoteMappings(validVrfRemote));
    }

    @Test
    public void testBridgeDomainRemoteValid() {
        assertEquals(BRIDGE_DOMAIN_MAPPINGS_ONLY, subtableFilterForRemoteMappings(validBdRemote));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoteInvalid() {
        subtableFilterForRemoteMappings(invalidRemote);
    }
}