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

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315._interface.role.grouping.RolesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315._interface.role.grouping.roles.Role;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315._interface.role.grouping.roles.RoleBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.role.rev170315._interface.role.grouping.roles.RoleKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceRoleReadCustomizer implements ListReaderCustomizer<Role, RoleKey, RoleBuilder> {

    @Nonnull
    @Override
    public List<RoleKey> getAllIds(@Nonnull final InstanceIdentifier<Role> instanceIdentifier,
                                   @Nonnull final ReadContext readContext) throws ReadFailedException {
        throw new UnsupportedOperationException("Operational read not supported for interface roles");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Role> list) {
        ((RolesBuilder) builder).setRole(list);
    }

    @Nonnull
    @Override
    public RoleBuilder getBuilder(@Nonnull final InstanceIdentifier<Role> instanceIdentifier) {
        return new RoleBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Role> instanceIdentifier,
                                      @Nonnull final RoleBuilder roleBuilder, @Nonnull final ReadContext readContext)
            throws ReadFailedException {
        throw new UnsupportedOperationException("Operational read not supported for interface roles");
    }
}
