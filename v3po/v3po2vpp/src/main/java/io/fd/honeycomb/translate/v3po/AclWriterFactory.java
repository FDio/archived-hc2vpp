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

package io.fd.honeycomb.translate.v3po;

import static io.fd.honeycomb.translate.v3po.InterfacesWriterFactory.IETF_ACL_ID;
import static io.fd.honeycomb.translate.v3po.SubinterfaceAugmentationWriterFactory.SUBIF_IETF_ACL_ID;

import com.google.common.collect.Sets;
import io.fd.honeycomb.translate.impl.write.GenericListWriter;
import io.fd.honeycomb.translate.v3po.interfaces.acl.IetfAclWriter;
import io.fd.honeycomb.translate.write.WriterFactory;
import io.fd.honeycomb.translate.write.registry.ModifiableWriterRegistryBuilder;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.AccessListEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Actions;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.acl.access.list.entries.ace.Matches;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class AclWriterFactory implements WriterFactory {

    public static final InstanceIdentifier<Acl> ACL_ID =
        InstanceIdentifier.create(AccessLists.class).child(Acl.class);

    @Override
    public void init(@Nonnull final ModifiableWriterRegistryBuilder registry) {

        final InstanceIdentifier<Acl> aclIdRelative = InstanceIdentifier.create(Acl.class);

        final InstanceIdentifier<Ace> aceId = aclIdRelative.child(AccessListEntries.class).child(Ace.class);
        final InstanceIdentifier<Actions> actionsId = aceId.child(Actions.class);
        final InstanceIdentifier<Matches> matchesId = aceId.child(Matches.class);

        registry.subtreeAddBefore(Sets.newHashSet(aceId, actionsId, matchesId),
            new GenericListWriter<>(ACL_ID, new IetfAclWriter()),
            Sets.newHashSet(IETF_ACL_ID, SUBIF_IETF_ACL_ID));
    }
}
