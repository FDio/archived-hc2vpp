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

package io.fd.hc2vpp.nat.read.ifc;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.vpp.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractSubInterfaceNatCustomizer<C extends DataObject, B extends Builder<C>>
        extends AbstractInterfaceNatCustomizer<C, B> {
    AbstractSubInterfaceNatCustomizer(@Nonnull final FutureJVppNatFacade jvppNat,
                                      @Nonnull final NamingContext ifcContext,
                                      @Nonnull final VppAttributesBuilder vppAttributesBuilder) {
        super(jvppNat, ifcContext, vppAttributesBuilder);
    }

    @Override
    protected String getName(final InstanceIdentifier<C> id) {
        // TODO(HC2VPP-99): use SubInterfaceUtils after it is moved from v3po2vpp
        final String parentInterfaceName =
                checkNotNull(id.firstKeyOf(Interface.class), "operational Interface identifier expected").getName();
        final Long subIfId = id.firstKeyOf(SubInterface.class).getIdentifier();
        return String.format("%s.%d", parentInterfaceName, subIfId.intValue());
    }
}
