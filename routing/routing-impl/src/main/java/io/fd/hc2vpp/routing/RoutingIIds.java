/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.StaticRoutes1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv4.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv4.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.Ipv61;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.Ipv6RouterAdvertisements;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.ipv6.router.advertisements.PrefixList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.interfaces._interface.ipv6.ipv6.router.advertisements.prefix.list.Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.Ipv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.Routing;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.ControlPlaneProtocols;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.ControlPlaneProtocol;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev180313.routing.control.plane.protocols.control.plane.protocol.StaticRoutes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class RoutingIIds {

    //Reader IIDs
    public static final InstanceIdentifier<Routing> ROUTING = InstanceIdentifier.create(Routing.class);
    public static final InstanceIdentifier<ControlPlaneProtocols> RT_CPS = ROUTING.child(ControlPlaneProtocols.class);
    public static final InstanceIdentifier<ControlPlaneProtocol> RT_CPS_CP = RT_CPS.child(ControlPlaneProtocol.class);

    public static final InstanceIdentifier<StaticRoutes> RT_CPS_CP_SR = RT_CPS_CP.child(StaticRoutes.class);
    public static final InstanceIdentifier<Route> RT_CPS_CP_SR_SRV4_IPV4_RT =
            RT_CPS_CP_SR.augmentation(StaticRoutes1.class).child(Ipv4.class).child(Route.class);
    public static final InstanceIdentifier<Route> RT_CPS_CP_SR_SRV4_IPV4_RT_PARENT =
            InstanceIdentifier.create(Route.class);

    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route>
            RT_CPS_CP_SR_SRV6_IPV6_RT = RT_CPS_CP_SR.augmentation(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.StaticRoutes1.class)
            .child(Ipv6.class)
            .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route.class);
    public static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route>
            RT_CPS_CP_SR_SRV6_IPV6_RT_PARENT = InstanceIdentifier.create(
                    org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipv6.unicast.routing.rev180313.routing.control.plane.protocols.control.plane.protocol._static.routes.ipv6.Route.class);

    //Writer IIDs
    public static final InstanceIdentifier<Ipv6RouterAdvertisements> IFCS_IFC_IFC1_IPV6_IPV61_RTADV =
            InstanceIdentifier.create(Interfaces.class).child(Interface.class).augmentation(Interface1.class)
                    .child(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv6.class)
                    .augmentation(Ipv61.class).child(Ipv6RouterAdvertisements.class);
    public static final InstanceIdentifier<Prefix> IFCS_IFC_IFC1_IPV6_IPV61_RTADV_PRLST_PRFX =
            IFCS_IFC_IFC1_IPV6_IPV61_RTADV.child(PrefixList.class).child(Prefix.class);
}
