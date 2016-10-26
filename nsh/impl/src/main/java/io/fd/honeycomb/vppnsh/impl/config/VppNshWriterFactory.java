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

package io.fd.honeycomb.vppnsh.impl.config;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.vpp.jvpp.nsh.future.FutureJVppNsh;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType1Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.NshMdType2Augment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.VppNsh;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.NshMaps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.entries.NshEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nsh.rev160624.vpp.nsh.nsh.maps.NshMap;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppNshWriterFactory implements WriterFactory {

    @Nonnull
    private final FutureJVppNsh jvppNsh;
    private final NamingContext nshEntryContext;
    private final NamingContext nshMapContext;
    private final NamingContext interfaceContext;

    @Inject
    public VppNshWriterFactory(@Nonnull final FutureJVppNsh jvppNsh,
                               @Named("nsh-entry-context") @Nonnull final NamingContext nshEntryContext,
                               @Named("nsh-map-context") @Nonnull final NamingContext nshMapContext,
                               @Named("interface-context") @Nonnull final NamingContext interfaceContext) {
        this.jvppNsh = jvppNsh;
        this.nshEntryContext = nshEntryContext;
        this.nshMapContext = nshMapContext;
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // WriterFactory is intended for registering Writers into HC framework
        // Writers handle ONLY config (config "true") data coming from upper layers and propagate them into lower layer/device
        // they are triggered when RESTCONF PUT/POST on config is invoked or when NETCONF edit-config + commit operation is executed

        // VppNsh has no handlers
        //  NshEntries has no handlers
        //   NshEntry =
        final InstanceIdentifier<NshEntries> nshEntriesId = InstanceIdentifier.create(VppNsh.class).child(NshEntries.class);
        final InstanceIdentifier<NshEntry> nshEntryId = nshEntriesId.child(NshEntry.class);
        registry.subtreeAdd(
                Sets.newHashSet(
                    InstanceIdentifier.create(NshEntry.class).augmentation(NshMdType1Augment.class),
                    InstanceIdentifier.create(NshEntry.class).augmentation(NshMdType2Augment.class)),
                new GenericListWriter<>(nshEntryId, new NshEntryWriterCustomizer(jvppNsh, nshEntryContext)));

        // VppNsh has no handlers
        //  NshMaps has no handlers
        //   NshMap =
        final InstanceIdentifier<NshMap> nshMapId =
              InstanceIdentifier.create(VppNsh.class).child(NshMaps.class).child(NshMap.class);
        registry.add(new GenericListWriter<>(nshMapId, new NshMapWriterCustomizer(jvppNsh, nshMapContext, interfaceContext)));

    }
}
