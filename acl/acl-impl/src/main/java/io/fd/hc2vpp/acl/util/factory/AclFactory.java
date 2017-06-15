/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.acl.util.factory;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160708.acl.transport.header.fields.SourcePortRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.VppAclAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.ace.VppAceNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.access.lists.acl.access.list.entries.ace.matches.ace.type.vpp.macip.ace.VppMacipAceNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.icmp.header.fields.IcmpCodeRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.icmp.header.fields.IcmpTypeRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.icmp.IcmpNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.icmp.v6.IcmpV6Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.other.OtherNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.tcp.TcpNodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev170615.acl.ip.protocol.header.fields.ip.protocol.udp.UdpNodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface AclFactory {

    default Set<InstanceIdentifier<?>> vppAclChildren(final InstanceIdentifier<Acl> parentId) {
        final InstanceIdentifier<Matches> matchesIid =
            parentId.child(AccessListEntries.class).child(Ace.class).child(Matches.class);
        return ImmutableSet.of(
            parentId.augmentation(VppAclAugmentation.class),
            parentId.child(AccessListEntries.class),
            parentId.child(AccessListEntries.class).child(Ace.class),
            parentId.child(AccessListEntries.class).child(Ace.class).child(Matches.class),
            parentId.child(AccessListEntries.class).child(Ace.class).child(Actions.class),
            matchesIid,
            matchesIid.child(VppMacipAceNodes.class),
            matchesIid.child(VppAceNodes.class),
            matchesIid.child(VppAceNodes.class).child(IcmpNodes.class),
            matchesIid.child(VppAceNodes.class).child(IcmpNodes.class).child(IcmpCodeRange.class),
            matchesIid.child(VppAceNodes.class).child(IcmpNodes.class).child(IcmpTypeRange.class),
            matchesIid.child(VppAceNodes.class).child(IcmpV6Nodes.class),
            matchesIid.child(VppAceNodes.class).child(IcmpV6Nodes.class).child(IcmpCodeRange.class),
            matchesIid.child(VppAceNodes.class).child(IcmpV6Nodes.class).child(IcmpTypeRange.class),
            matchesIid.child(VppAceNodes.class).child(UdpNodes.class),
            matchesIid.child(VppAceNodes.class).child(UdpNodes.class).child(SourcePortRange.class),
            matchesIid.child(VppAceNodes.class).child(UdpNodes.class).child(DestinationPortRange.class),
            matchesIid.child(VppAceNodes.class).child(TcpNodes.class),
            matchesIid.child(VppAceNodes.class).child(TcpNodes.class).child(SourcePortRange.class),
            matchesIid.child(VppAceNodes.class).child(TcpNodes.class).child(DestinationPortRange.class),
            matchesIid.child(VppAceNodes.class).child(OtherNodes.class)

        );
    }

}
