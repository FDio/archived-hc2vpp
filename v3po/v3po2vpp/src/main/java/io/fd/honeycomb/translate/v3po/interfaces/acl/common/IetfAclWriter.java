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

package io.fd.honeycomb.translate.v3po.interfaces.acl.common;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.List;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.access.lists.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface IetfAclWriter {
    default void write(@Nonnull final InstanceIdentifier<?> id, final int ifIndex, @Nonnull final List<Acl> acls,
                       final AccessLists.DefaultAction defaultAction, @Nullable final InterfaceMode mode,
                       @Nonnull final WriteContext writeContext, @Nonnull final MappingContext mappingContext)
        throws WriteFailedException {
        write(id, ifIndex, acls, defaultAction, mode, writeContext, 0, mappingContext);
    }

    void write(@Nonnull final InstanceIdentifier<?> id, int ifIndex, @Nonnull final List<Acl> acls,
               final AccessLists.DefaultAction defaultAction, @Nullable InterfaceMode mode,
               @Nonnull final WriteContext writeContext, @Nonnegative final int numberOfTags,
               @Nonnull final MappingContext mappingContext)
        throws WriteFailedException;

    void deleteAcl(@Nonnull final InstanceIdentifier<?> id, int ifIndex, @Nonnull final MappingContext mappingContext)
        throws WriteFailedException;
}
