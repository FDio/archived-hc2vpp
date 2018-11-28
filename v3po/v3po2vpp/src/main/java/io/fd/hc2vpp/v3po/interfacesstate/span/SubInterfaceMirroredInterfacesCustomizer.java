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
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.subinterface.span.rev170607.VppSubinterfaceSpanAugmentation;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.subinterface.span.rev170607.interfaces._interface.sub.interfaces.sub._interface.Span;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.subinterface.span.rev170607.interfaces.state._interface.sub.interfaces.sub._interface.SpanStateBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.span.state.attributes.MirroredInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
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
        final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.span.attributes.MirroredInterfaces> cfgId =
                SubInterfaceCustomizer.getCfgId(RWUtils.cutId(id, SubInterface.class))
                        .augmentation(VppSubinterfaceSpanAugmentation.class)
                        .child(Span.class)
                        .child(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.span.attributes.MirroredInterfaces.class);

        final org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.span.attributes.MirroredInterfaces
                cfgValue = new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.span.attributes.MirroredInterfacesBuilder()
                .setMirroredInterface(
                        Optional.ofNullable(readValue.getMirroredInterface()).orElse(Collections.emptyList())
                                .stream()
                                .map(mirroredInterface -> new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.span.attributes.mirrored.interfaces.MirroredInterfaceBuilder()
                                        .withKey(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.span.attributes.mirrored.interfaces.MirroredInterfaceKey(mirroredInterface.key().getIfaceRef()))
                                        .setIfaceRef(mirroredInterface.getIfaceRef())
                                        .setState(mirroredInterface.getState())
                                        .build())
                                .collect(Collectors.toList()))
                .build();

        return Initialized.create(cfgId, cfgValue);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull MirroredInterfaces mirroredInterfaces) {
        ((SpanStateBuilder) builder).setMirroredInterfaces(mirroredInterfaces);
    }
}
