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

package io.fd.hc2vpp.v3po.interfacesstate.span;


import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.SubInterfaceCustomizer;
import io.fd.hc2vpp.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.subinterface.span.rev170510.VppSubinterfaceSpanAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.subinterface.span.rev170510.interfaces._interface.sub.interfaces.sub._interface.Span;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.subinterface.span.rev170510.interfaces.state._interface.sub.interfaces.sub._interface.SpanStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev170509.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provides sub-interface-specific logic to read/init port mirroring configuration
 */
public class SubInterfaceMirroredInterfacesCustomizer extends AbstractMirroredInterfacesCustomizer {

    public SubInterfaceMirroredInterfacesCustomizer(@Nonnull FutureJVppCore futureJVppCore, NamingContext ifcContext) {
        super(futureJVppCore, ifcContext, SubInterfaceUtils::subInterfaceFullNameOperational);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<MirroredInterfaces> id,
                                                  @Nonnull MirroredInterfaces readValue,
                                                  @Nonnull ReadContext readContext) {
        final InstanceIdentifier<MirroredInterfaces> cfgId =
                SubInterfaceCustomizer.getCfgId(RWUtils.cutId(id, SubInterface.class))
                        .augmentation(VppSubinterfaceSpanAugmentation.class)
                        .child(Span.class)
                        .child(MirroredInterfaces.class);

        return Initialized.create(cfgId, readValue);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull MirroredInterfaces mirroredInterfaces) {
        ((SpanStateBuilder) builder).setMirroredInterfaces(mirroredInterfaces);
    }
}
