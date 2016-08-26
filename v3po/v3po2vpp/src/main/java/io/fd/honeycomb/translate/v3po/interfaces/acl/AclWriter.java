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

package io.fd.honeycomb.translate.v3po.interfaces.acl;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.AclBase;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160708.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer customizer responsible for Access Control Lists management. Does not send any messages to VPP. All the config
 * data are stored in HC and used when acl is assigned/unassigned to/from an interface.
 *
 * ACLs that are currently assigned to an interface cannot be updated/deleted.
 */
public class AclWriter implements ListWriterCustomizer<Acl, AclKey> {

    public static final InstanceIdentifier<AccessLists> ACL_ID =
        InstanceIdentifier.create(AccessLists.class);

    private static final Logger LOG = LoggerFactory.getLogger(AclWriter.class);

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Creating ACL: iid={} dataAfter={}", id, dataAfter);

        // no vpp call, just updates DataTree
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final Acl dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Updating ACL: iid={} dataBefore={} dataAfter={}", id, dataBefore, dataAfter);

        if (isAssigned(dataAfter, writeContext)) {
            throw new WriteFailedException(id,
                String.format("Failed to update data at %s: acl %s is already assigned", id, dataAfter));
        }

        LOG.debug("Updating unassigned ACL: iid={} dataBefore={} dataAfter={}", id, dataBefore, dataAfter);

        // no vpp call, just updates DataTree
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Deleting ACL: iid={} dataBefore={}", id, dataBefore);

        if (isAssigned(dataBefore, writeContext)) {
            throw new WriteFailedException(id,
                String.format("Failed to delete data at %s: acl %s is already assigned", id, dataBefore));
        }

        LOG.debug("Deleting unassigned ACL: iid={} dataBefore={}", id, dataBefore);

        // no vpp call, just updates DataTree
    }

    private static boolean isAssigned(@Nonnull final Acl acl,
                                      @Nonnull final WriteContext writeContext) {
        final String aclName = acl.getAclName();
        final Class<? extends AclBase> aclType = acl.getAclType();
        final Interfaces interfaces = writeContext.readAfter(InstanceIdentifier.create(Interfaces.class)).get();

        return interfaces.getInterface().stream()
            .map(i -> Optional.ofNullable(i.getAugmentation(VppInterfaceAugmentation.class))
                .map(aug -> aug.getIetfAcl())
                .map(ietfAcl -> ietfAcl.getAccessLists())
                .map(accessLists -> accessLists.getAcl())
            )
            .flatMap(iacl -> iacl.isPresent()
                ? iacl.get().stream()
                : Stream.empty())
            .filter(assignedAcl -> aclName.equals(assignedAcl.getName()) && aclType.equals(assignedAcl.getType()))
            .findFirst().isPresent();
    }
}
