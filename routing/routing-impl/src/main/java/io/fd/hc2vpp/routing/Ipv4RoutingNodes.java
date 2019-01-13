/*
 * Copyright (c) 2016 Cisco, Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.routing;

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4NextHopAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.VppIpv4RouteAttributesAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipv4.unicast.routing.rev180319.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.VppIpv4Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.NextHop;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.next.hop.NextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.route.next.hop.SimpleNextHop1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.NextHopList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface Ipv4RoutingNodes extends JvppReplyConsumer {

    @SuppressWarnings("unchecked")
    default Set<InstanceIdentifier<?>> ipv4RoutingHandledChildren(final InstanceIdentifier<Route> parent) {
        return ImmutableSet.of(
            parent.child(NextHop.class),
            parent.child(NextHop.class).augmentation((Class)NextHopList.class),
            parent.child(NextHop.class).augmentation((Class)NextHopList.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop.class),
            parent.child(NextHop.class).augmentation((Class)NextHopList.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop.class)
                .augmentation(NextHop1.class),
            parent.child(NextHop.class).augmentation((Class)NextHopList.class)
                .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.next.hop.content.next.hop.options.next.hop.list.next.hop.list.NextHop.class)
                .augmentation(VppIpv4NextHopAugmentation.class),
            parent.child(NextHop.class).augmentation((Class)SimpleNextHop1.class),
            parent.augmentation(VppIpv4RouteAttributesAugmentation.class).child(VppIpv4Route.class));
    }

}
