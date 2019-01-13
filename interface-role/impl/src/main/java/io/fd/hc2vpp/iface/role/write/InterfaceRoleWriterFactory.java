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

package io.fd.hc2vpp.iface.role.write;

import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.role.rev170615.InterfaceRoleAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.role.rev170615._interface.role.grouping.Roles;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang._interface.role.rev170615._interface.role.grouping.roles.Role;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceRoleWriterFactory implements WriterFactory {

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registryBuilder) {
        final InstanceIdentifier<Role> roleId = InstanceIdentifier.create(Interfaces.class)
                .child(Interface.class).augmentation(InterfaceRoleAugmentation.class)
                .child(Roles.class).child(Role.class);
        registryBuilder.add(new GenericListWriter<>(roleId, new InterfaceRoleWriteCustomizer()));
    }
}
