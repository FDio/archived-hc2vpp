/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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


package io.fd.hc2vpp.acl;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppAclAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppIcmpAceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppTcpAceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acl.icmp.header.fields.IcmpCodeRange;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acl.icmp.header.fields.IcmpTypeRange;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acls.acl.aces.ace.matches.l4.icmp.icmp.VppIcmpAce;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.acls.acl.aces.ace.matches.l4.tcp.tcp.VppTcpAce;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.Acls;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.Aces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.Matches;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l2.eth.Eth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv4.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l3.ipv6.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.icmp.Icmp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.Tcp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.DestinationPort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.tcp.tcp.SourcePort;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.Udp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.Egress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.Ingress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSets;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AclIIds {
    public static final InstanceIdentifier<Acls> ACLS = InstanceIdentifier.create(Acls.class);
    public static final InstanceIdentifier<Acl> ACLS_ACL = ACLS.child(Acl.class);
    public static final InstanceIdentifier<Acl> ACL = InstanceIdentifier.create(Acl.class);

    public static final InstanceIdentifier<AttachmentPoints> ACLS_AP = ACLS.child(AttachmentPoints.class);
    public static final InstanceIdentifier<Interface> ACLS_AP_INT = ACLS_AP.child(Interface.class);
    public static final InstanceIdentifier<Ingress> ACLS_AP_INT_ING = ACLS_AP_INT.child(Ingress.class);
    public static final InstanceIdentifier<AclSets> ACLS_AP_INT_ING_ACLS = ACLS_AP_INT_ING.child(AclSets.class);
    public static final InstanceIdentifier<AclSet> ACLS_AP_INT_ING_ACLS_ACL = ACLS_AP_INT_ING_ACLS.child(AclSet.class);
    public static final InstanceIdentifier<Egress> ACLS_AP_INT_EGR = ACLS_AP_INT.child(Egress.class);
    public static final InstanceIdentifier<AclSets> ACLS_AP_INT_EGR_ACLS = ACLS_AP_INT_EGR.child(AclSets.class);
    public static final InstanceIdentifier<AclSet> ACLS_AP_INT_EGR_ACLS_ACL = ACLS_AP_INT_EGR_ACLS.child(AclSet.class);
    public static final InstanceIdentifier<Interface> IFC_ACL = InstanceIdentifier.create(Interface.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface>
            IFC = InstanceIdentifier.create(Interfaces.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface>
            IFC_STATE = InstanceIdentifier.create(InterfacesState.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface.class);


    public static Set<InstanceIdentifier<?>> vppAclChildren(final InstanceIdentifier<Acl> parentId) {
        final InstanceIdentifier<Matches> matchesIid =
                parentId.child(Aces.class).child(Ace.class).child(Matches.class);
        return ImmutableSet.of(
                parentId.augmentation(VppAclAugmentation.class),
                parentId.child(Aces.class),
                parentId.child(Aces.class).child(Ace.class),
                parentId.child(Aces.class).child(Ace.class).child(Actions.class),
                matchesIid,
                matchesIid.child(Eth.class),
                matchesIid.child(Ipv4.class),
                matchesIid.child(Ipv6.class),
                matchesIid.child(Tcp.class),
                matchesIid.child(Tcp.class).augmentation(VppTcpAceAugmentation.class),
                matchesIid.child(Tcp.class).augmentation(VppTcpAceAugmentation.class).child(VppTcpAce.class),
                matchesIid.child(Tcp.class).child(DestinationPort.class),
                matchesIid.child(Tcp.class).child(SourcePort.class),
                matchesIid.child(Udp.class),
                matchesIid.child(Udp.class).child(
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.DestinationPort.class),
                matchesIid.child(Udp.class).child(
                        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.ace.matches.l4.udp.udp.SourcePort.class),
                matchesIid.child(Icmp.class),
                matchesIid.child(Icmp.class).augmentation(VppIcmpAceAugmentation.class),
                matchesIid.child(Icmp.class).augmentation(VppIcmpAceAugmentation.class).child(VppIcmpAce.class),
                matchesIid.child(Icmp.class).augmentation(VppIcmpAceAugmentation.class).child(VppIcmpAce.class)
                        .child(IcmpTypeRange.class),
                matchesIid.child(Icmp.class).augmentation(VppIcmpAceAugmentation.class).child(VppIcmpAce.class)
                        .child(IcmpCodeRange.class)
        );
    }

    public static Set<InstanceIdentifier<?>> aclHandledChildren(final InstanceIdentifier<Interface> parentId) {
        return ImmutableSet.of(
                parentId.child(Ingress.class),
                parentId.child(Ingress.class).child(AclSets.class),
                parentId.child(Ingress.class).child(AclSets.class).child(AclSet.class),
                parentId.child(Egress.class),
                parentId.child(Egress.class).child(AclSets.class),
                parentId.child(Egress.class).child(AclSets.class).child(AclSet.class));
    }
}
