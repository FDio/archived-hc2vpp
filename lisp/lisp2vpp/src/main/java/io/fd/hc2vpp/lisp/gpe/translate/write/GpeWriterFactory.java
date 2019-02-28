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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.lisp.gpe.GpeModule;
import io.fd.hc2vpp.lisp.gpe.translate.service.GpeStateCheckService;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.impl.write.GenericWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.Gpe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.NativeForwardPathsTables;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables.NativeForwardPathsTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.GpeEntryTable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.GpeEntry;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.LocalEid;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.entry.table.grouping.gpe.entry.table.gpe.entry.RemoteEid;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.gpe.feature.data.grouping.GpeFeatureData;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.gpe.rev170801.locator.pairs.grouping.LocatorPair;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.SubinterfaceAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class GpeWriterFactory implements WriterFactory {

    private static final InstanceIdentifier<Gpe> GPE_ID = InstanceIdentifier.create(Gpe.class);
    private static final InstanceIdentifier<GpeFeatureData>
            GPE_FEATURE_ID = GPE_ID.child(GpeFeatureData.class);
    private static final InstanceIdentifier<GpeEntry>
            GPE_ENTRY_ID = GPE_FEATURE_ID.child(GpeEntryTable.class).child(GpeEntry.class);
    private static final InstanceIdentifier<Interface>
            IFC_ID = InstanceIdentifier.create(Interfaces.class).child(Interface.class);

    @Inject
    private FutureJVppCore api;

    @Inject
    private GpeStateCheckService gpeStateCheckService;

    @Inject
    @Named(GpeModule.GPE_ENTRY_MAPPING_CTX)
    private NamingContext gpeEntryMappingContext;

    @Inject
    @Named("interface-context")
    private NamingContext interfaceContext;


    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // gpe must be enabled before interfaces
        // because as a byproduct of enabling gpe, lisp_gpe interface is created
        // and in scenario when vpp data are lost, it would end up calling
        // sw_interface_set_flags for non existing interface index
        registry.addBefore(new GenericWriter<>(GPE_FEATURE_ID, new GpeFeatureCustomizer(api)),
                IFC_ID);
        final InstanceIdentifier<GpeEntry> entrySubtreeId = InstanceIdentifier.create(GpeEntry.class);

        // same situation as above, but with sub-interfaces
        registry.subtreeAddBefore(ImmutableSet.of(
                entrySubtreeId.child(LocalEid.class),
                entrySubtreeId.child(RemoteEid.class),
                entrySubtreeId.child(LocatorPair.class)),
                new GenericListWriter<>(GPE_ENTRY_ID,
                        new GpeForwardEntryCustomizer(api, gpeStateCheckService, gpeEntryMappingContext)),
                IFC_ID.augmentation(SubinterfaceAugmentation.class).child(SubInterfaces.class)
                        .child(SubInterface.class));

        final InstanceIdentifier<NativeForwardPathsTable> nativeEntryTableId =
                InstanceIdentifier.create(NativeForwardPathsTables.class).child(NativeForwardPathsTable.class);
        // gpe_add_del_iface is used to create fib table, so must be written before interfaces, to ensure
        // byproduct iface is created before there's an attempt to set its flags
        registry.addBefore(new GenericListWriter<>(nativeEntryTableId, new NativeForwardPathsTableCustomizer(api)),
                IFC_ID);
        registry.add(new GenericListWriter<>(nativeEntryTableId.child(NativeForwardPath.class),
                new NativeForwardPathCustomizer(api, interfaceContext)));
    }
}
