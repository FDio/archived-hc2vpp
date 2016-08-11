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

package io.fd.honeycomb.samples.interfaces.mapping.config;

import com.google.inject.Inject;
import io.fd.honeycomb.samples.interfaces.mapping.LowerLayerAccess;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.Interfaces;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfacesWriterFactory implements WriterFactory {

    @Nonnull
    private final LowerLayerAccess access;

    @Inject
    public InterfacesWriterFactory(@Nonnull final LowerLayerAccess access) {
        this.access = access;
    }

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {
        // ReaderFactory is intended for registering Writers into HC framework
        // Writers handle ONLY config (config "true") data coming from upper layers and propagate them into lower layer/device
        // they are triggered when RESTCONF PUT/POST on config is invoked or when NETCONF edit-config + commit operation is executed

        // Our model root for operational data is Interfaces
        final InstanceIdentifier<Interfaces> root = InstanceIdentifier.create(Interfaces.class);
        // But unlike ReaderFactories, there's no need to create a structural writer, we can "ignore" any nodes
        // that do not contain actual data (leaves)

        // Next child node is Interface (list)
        final InstanceIdentifier<Interface> ifcListId = root.child(Interface.class);
        registry.add(new GenericListWriter<>(ifcListId, new InterfaceWriterCustomizer(access)));
    }
}
