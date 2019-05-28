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

package io.fd.hc2vpp.nat.write.ifc;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.nat.rev190527.InterfaceNatVppFeatureAttributes;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev190527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractSubInterfaceNatCustomizer<D extends InterfaceNatVppFeatureAttributes & DataObject>
        extends AbstractInterfaceNatCustomizer<D> {
    AbstractSubInterfaceNatCustomizer(@Nonnull final FutureJVppNatFacade jvppNat,
                                      @Nonnull final NamingContext ifcContext) {
        super(jvppNat, ifcContext);
    }

    @Override
    protected String getName(final InstanceIdentifier<D> id) {
        // TODO(HC2VPP-99): use SubInterfaceUtils after it is moved from v3po2vpp
        final String parentInterfaceName =
                checkNotNull(id.firstKeyOf(Interface.class), "Interface configuration identifier expected").getName();
        final Long subIfId = id.firstKeyOf(SubInterface.class).getIdentifier();
        return String.format("%s.%d", parentInterfaceName, subIfId.intValue());
    }
}
