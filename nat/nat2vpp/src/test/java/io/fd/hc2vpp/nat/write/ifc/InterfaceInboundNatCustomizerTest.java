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

package io.fd.hc2vpp.nat.write.ifc;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev190527.NatInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev190527._interface.nat.attributes.Nat;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev190527._interface.nat.attributes.nat.Inbound;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev190527._interface.nat.attributes.nat.InboundBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceInboundNatCustomizerTest
        extends AbstractNatCustomizerTest<Inbound, InterfaceInboundNatCustomizer> {

    @Override
    protected Inbound getPreRoutingConfig() {
        return new InboundBuilder().setPostRouting(false).setNat44Support(true).build();
    }

    @Override
    protected Inbound getPostRoutingConfig() {
        return new InboundBuilder().setPostRouting(true).setNat44Support(true).build();
    }

    @Override
    protected InstanceIdentifier<Inbound> getIId(final String ifaceName) {
        return InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class, new InterfaceKey(ifaceName)).augmentation(NatInterfaceAugmentation.class)
                .child(Nat.class).child(Inbound.class);
    }

    @Override
    protected InterfaceInboundNatCustomizer getCustomizer(final FutureJVppNatFacade natApi,
                                                          final NamingContext ifcNamingCtx) {
        return new InterfaceInboundNatCustomizer(natApi, ifcNamingCtx);
    }
}
