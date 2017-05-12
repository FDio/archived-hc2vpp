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
import io.fd.hc2vpp.v3po.interfacesstate.InterfaceCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces.state._interface.SpanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.span.attributes.MirroredInterfaces;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provides interface-specific logic to read/init port mirroring configuration
 */
public class InterfaceMirroredInterfacesCustomizer extends AbstractMirroredInterfacesCustomizer {

    public InterfaceMirroredInterfacesCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                                 @Nonnull final NamingContext ifcContext) {
        super(futureJVppCore, ifcContext, id -> id.firstKeyOf(Interface.class).getName());
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<MirroredInterfaces> id,
                                                  @Nonnull final MirroredInterfaces readValue,
                                                  @Nonnull final ReadContext ctx) {
        final InstanceIdentifier<MirroredInterfaces> cfgId =
                InterfaceCustomizer.getCfgId(RWUtils.cutId(id, Interface.class))
                        .augmentation(VppInterfaceAugmentation.class)
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170315.interfaces._interface.Span.class)
                        .child(MirroredInterfaces.class);
        return Initialized.create(cfgId, readValue);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull MirroredInterfaces mirroredInterfaces) {
        ((SpanBuilder) builder).setMirroredInterfaces(mirroredInterfaces);
    }
}
