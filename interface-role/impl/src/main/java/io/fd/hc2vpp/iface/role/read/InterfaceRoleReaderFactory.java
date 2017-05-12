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

package io.fd.hc2vpp.iface.role.read;

import io.fd.honeycomb.translate.impl.read.GenericListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315.InterfaceRoleStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315.InterfaceRoleStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315._interface.role.grouping.Roles;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315._interface.role.grouping.RolesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315._interface.role.grouping.roles.Role;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceRoleReaderFactory implements ReaderFactory {

    @Override
    public void init(@Nonnull final ModifiableReaderRegistryBuilder registry) {
        final InstanceIdentifier<InterfaceRoleStateAugmentation> augId =
                InstanceIdentifier.create(InterfacesState.class)
                        .child(Interface.class)
                        .augmentation(InterfaceRoleStateAugmentation.class);
        registry.addStructuralReader(augId, InterfaceRoleStateAugmentationBuilder.class);

        final InstanceIdentifier<Roles> rolesId = augId.child(Roles.class);
        registry.addStructuralReader(rolesId, RolesBuilder.class);
        registry.add(new GenericListReader<>(rolesId.child(Role.class), new InterfaceRoleReadCustomizer()));
    }
}
