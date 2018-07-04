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

package io.fd.hc2vpp.nat;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.Nat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.Instances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.MappingTable;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.mapping.table.MappingEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64Prefixes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface NatIds {
    InstanceIdentifier<Nat> NAT_ID = InstanceIdentifier.create(Nat.class);
    InstanceIdentifier<Instances> NAT_INSTANCES_ID = NAT_ID.child(Instances.class);
    InstanceIdentifier<Instance> NAT_INSTANCE_ID = NAT_INSTANCES_ID.child(Instance.class);
    InstanceIdentifier<MappingTable> MAPPING_TABLE_ID = NAT_INSTANCE_ID.child(MappingTable.class);
    InstanceIdentifier<MappingEntry> MAPPING_ENTRY_ID = MAPPING_TABLE_ID.child(MappingEntry.class);
    InstanceIdentifier<Policy> POLICY_ID = NAT_INSTANCE_ID.child(Policy.class);
    InstanceIdentifier<ExternalIpAddressPool> ADDRESS_POOL_ID = POLICY_ID.child(ExternalIpAddressPool.class);
    InstanceIdentifier<Nat64Prefixes> NAT64_PREFIXES_ID = POLICY_ID.child(Nat64Prefixes.class);
}
