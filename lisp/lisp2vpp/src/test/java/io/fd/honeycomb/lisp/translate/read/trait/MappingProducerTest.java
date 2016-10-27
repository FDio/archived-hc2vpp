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


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv4Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.Ipv6Afi;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.lisp.address.types.rev151105.MacAfi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.LocalMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.RemoteMappings;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.LocalMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.local.mappings.local.mapping.EidBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.RemoteMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MappingProducerTest implements MappingProducer {

    private InstanceIdentifier<LocalMapping> validVrfLocalMappingId;
    private InstanceIdentifier<LocalMapping> validBridgeDomainLocalMappingId;
    private InstanceIdentifier<RemoteMapping> validVrfRemoteMappingId;
    private InstanceIdentifier<RemoteMapping> validBridgeDomainRemoteMappingId;

    private LocalMapping ipv4LocalMapping;
    private LocalMapping ipv6LocalMapping;
    private LocalMapping macLocalMapping;

    private RemoteMapping ipv4RemoteMapping;
    private RemoteMapping ipv6RemoteMapping;
    private RemoteMapping macRemoteMapping;

    @Before
    public void init() {
        validVrfLocalMappingId = InstanceIdentifier.create(VrfSubtable.class)
                .child(LocalMappings.class)
                .child(LocalMapping.class);

        validBridgeDomainLocalMappingId = InstanceIdentifier.create(BridgeDomainSubtable.class)
                .child(LocalMappings.class)
                .child(LocalMapping.class);

        validVrfRemoteMappingId = InstanceIdentifier.create(VrfSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class);

        validBridgeDomainRemoteMappingId = InstanceIdentifier.create(BridgeDomainSubtable.class)
                .child(RemoteMappings.class)
                .child(RemoteMapping.class);

        ipv4LocalMapping = new LocalMappingBuilder()
                .setEid(new EidBuilder()
                        .setAddressType(Ipv4Afi.class)
                        .build()).build();

        ipv6LocalMapping = new LocalMappingBuilder()
                .setEid(new EidBuilder()
                        .setAddressType(Ipv6Afi.class)
                        .build()).build();
        macLocalMapping = new LocalMappingBuilder()
                .setEid(new EidBuilder()
                        .setAddressType(MacAfi.class)
                        .build()).build();

        ipv4RemoteMapping = new RemoteMappingBuilder()
                .setEid(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddressType(Ipv4Afi.class).build()).build();

        ipv6RemoteMapping = new RemoteMappingBuilder()
                .setEid(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddressType(Ipv6Afi.class).build()).build();

        macRemoteMapping = new RemoteMappingBuilder()
                .setEid(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.dp.subtable.grouping.remote.mappings.remote.mapping.EidBuilder()
                        .setAddressType(MacAfi.class).build()).build();
    }

    @Test
    public void testValidVrfLocal() {
        testPositiveCase(validVrfLocalMappingId, ipv4LocalMapping);
        testPositiveCase(validVrfLocalMappingId, ipv6LocalMapping);
    }

    @Test
    public void testValidBridgeDomainLocal() {
        testPositiveCase(validBridgeDomainLocalMappingId, macLocalMapping);
    }

    @Test
    public void testInvalidVrfLocal() {
        testNegativeCase(validVrfLocalMappingId, macLocalMapping);
    }

    @Test
    public void testInvalidBridgeDomainLocal() {
        testNegativeCase(validBridgeDomainLocalMappingId, ipv4LocalMapping);
        testNegativeCase(validBridgeDomainLocalMappingId, ipv6LocalMapping);
    }


    @Test
    public void testValidVrfRemote() {
        testPositiveCase(validVrfRemoteMappingId, ipv4RemoteMapping);
        testPositiveCase(validVrfRemoteMappingId, ipv6RemoteMapping);
    }

    @Test
    public void testValidBridgeDomainRemote() {
        testPositiveCase(validBridgeDomainRemoteMappingId, macRemoteMapping);
    }

    @Test
    public void testInvalidVrfRemote() {
        testNegativeCase(validVrfRemoteMappingId, macRemoteMapping);
    }

    @Test
    public void testInvalidBridgeDomainRemote() {
        testNegativeCase(validBridgeDomainRemoteMappingId, ipv4RemoteMapping);
        testNegativeCase(validBridgeDomainRemoteMappingId, ipv6RemoteMapping);
    }

    private void testNegativeCase(final InstanceIdentifier<LocalMapping> identifier, final LocalMapping data) {
        try {
            checkAllowedCombination(identifier, data);
        } catch (WriteFailedException e) {
            assertTrue(e instanceof WriteFailedException.CreateFailedException);
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            return;
        }

        fail("Test should have failed");
    }


    private void testPositiveCase(final InstanceIdentifier<LocalMapping> identifier, final LocalMapping data) {
        try {
            checkAllowedCombination(identifier, data);
        } catch (WriteFailedException e) {
            fail("Test should have passed");
        }
    }

    private void testNegativeCase(final InstanceIdentifier<RemoteMapping> identifier, final RemoteMapping data) {
        try {
            checkAllowedCombination(identifier, data);
        } catch (WriteFailedException e) {
            assertTrue(e instanceof WriteFailedException.CreateFailedException);
            assertTrue(e.getCause() instanceof IllegalArgumentException);
            return;
        }

        fail("Test should have failed");
    }


    private void testPositiveCase(final InstanceIdentifier<RemoteMapping> identifier, final RemoteMapping data) {
        try {
            checkAllowedCombination(identifier, data);
        } catch (WriteFailedException e) {
            fail("Test should have passed");
        }
    }
}
