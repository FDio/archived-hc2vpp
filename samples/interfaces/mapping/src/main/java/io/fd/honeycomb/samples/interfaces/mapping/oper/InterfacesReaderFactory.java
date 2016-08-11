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

package io.fd.honeycomb.samples.interfaces.mapping.oper;

import com.google.inject.Inject;
import io.fd.honeycomb.samples.interfaces.mapping.LowerLayerAccess;
import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.impl.read.GenericReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfacesState;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.InterfacesStateBuilder;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810._interface.state.Counters;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfacesReaderFactory implements ReaderFactory {

    @Nonnull
    private final LowerLayerAccess access;

    @Inject
    public InterfacesReaderFactory(@Nonnull final LowerLayerAccess access) {
        this.access = access;
    }

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        // ReaderFactory is intended for registering Readers into HC framework
        // Readers provide ONLY operational (config "false") data straight from underlying device/layer
        // they are triggered when RESTCONF GET on operational is invoked or when NETCONF get operation is executed

        // Our model root for operational data is InterfacesState
        final InstanceIdentifier<InterfacesState> root = InstanceIdentifier.create(InterfacesState.class);
        // Since InterfacesState has no direct data children (leaves) only a structural reader is registered
        // This reader just fills in the composite hierarchy of readers
        // Honeycomb can't automatically instantiate structural readers and plugins have to help it by invoking as:
        registry.addStructuralReader(root, InterfacesStateBuilder.class);

        // Next child node is Interface (list)
        final InstanceIdentifier<Interface> ifcListId = root.child(Interface.class);
        registry.add(new GenericListReader<>(ifcListId, new InterfaceReaderCustomizer(access)));

        // Next child is a container Counters
        final InstanceIdentifier<Counters> countersId = ifcListId.child(Counters.class);
        // By adding the reader with addAfter, we can ensure ordering of execution among the readers
        // Useful in cases when a certain read has to be invoked before/after another
        // In this case, we are ensuring that Counters are read after Interface is read
        // "add" could be used instead, leaving the ordering to "nature"
        // Same applies for writers
        registry.addAfter(new GenericReader<>(countersId, new CountersReaderCustomizer(access)), ifcListId);
    }
}
