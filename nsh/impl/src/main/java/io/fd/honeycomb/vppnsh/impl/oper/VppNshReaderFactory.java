/*
 * Copyright (c) 2016 Intel and/or its affiliates.
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

package io.fd.honeycomb.vppnsh.impl.oper;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNshStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.entries.NshEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.NshMapsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMap;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.state.nsh.maps.NshMapBuilder;

import io.fd.honeycomb.vppnsh.impl.oper.NshEntryReaderCustomizer;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.nsh.future.FutureJVppNsh;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppNshReaderFactory implements ReaderFactory {

    private final FutureJVppNsh jvppNsh;
    private final NamingContext nshEntryContext;
    private final NamingContext nshMapContext;
    private final NamingContext interfaceContext;

    @Inject
    public VppNshReaderFactory(final FutureJVppNsh jvppNsh,
                               @Named("nsh-entry-context") final NamingContext nshEntryContext,
                               @Named("nsh-map-context") final NamingContext nshMapContext,
                               @Named("interface-context") @Nonnull final NamingContext interfaceContext) {
        this.jvppNsh = jvppNsh;
        this.nshEntryContext = nshEntryContext;
        this.nshMapContext = nshMapContext;
        this.interfaceContext = interfaceContext;
    }
    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        // ReaderFactory is intended for registering Readers into HC framework
        // Readers provide ONLY operational (config "false") data straight from underlying device/layer
        // they are triggered when RESTCONF GET on operational is invoked or when NETCONF get operation is executed

        // VppNshState(Structural)
        final InstanceIdentifier<VppNshState> vppNshStateId = InstanceIdentifier.create(VppNshState.class);
        registry.addStructuralReader(vppNshStateId, VppNshStateBuilder.class);

        //  NshENtries(Structural)
        final InstanceIdentifier<NshEntries> nshEntriesId = vppNshStateId.child(NshEntries.class);
        registry.addStructuralReader(nshEntriesId, NshEntriesBuilder.class);
        //  NshENtry
        final InstanceIdentifier<NshEntry> nshEntryId = nshEntriesId.child(NshEntry.class);
        registry.add(new GenericListReader<>(nshEntryId, new NshEntryReaderCustomizer(jvppNsh, nshEntryContext)));

        //  NshMaps(Structural)
        final InstanceIdentifier<NshMaps> nshMapsId = vppNshStateId.child(NshMaps.class);
        registry.addStructuralReader(nshMapsId, NshMapsBuilder.class);
        //  NshMap
        final InstanceIdentifier<NshMap> nshMapId = nshMapsId.child(NshMap.class);
        registry.add(new GenericListReader<>(nshMapId, new NshMapReaderCustomizer(jvppNsh, nshMapContext, interfaceContext)));
    }
}
