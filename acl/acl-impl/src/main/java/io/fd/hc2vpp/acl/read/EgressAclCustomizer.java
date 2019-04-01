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


package io.fd.hc2vpp.acl.read;

import java.util.Optional;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EgressAclCustomizer extends AbstractAclCustomizer
        implements InitializingListReaderCustomizer<AclSet, AclSetKey, AclSetBuilder>, JvppReplyConsumer {

    public EgressAclCustomizer(final FutureJVppAclFacade futureAclFacade, final NamingContext interfaceContext,
                               final AclContextManager standardAclContext) {
        super(futureAclFacade, interfaceContext, standardAclContext);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<AclSet> instanceIdentifier,
                                                  @Nonnull final AclSet aclSet,
                                                  @Nonnull final ReadContext readContext) {
        return Initialized.create(instanceIdentifier, aclSet);
    }

    @Nonnull
    @Override
    public List<AclSetKey> getAllIds(@Nonnull final InstanceIdentifier<AclSet> instanceIdentifier,
                                     @Nonnull final ReadContext readContext) throws ReadFailedException {
        final String parentInterfaceName = instanceIdentifier.firstKeyOf(Interface.class).getInterfaceId();
        final int parentInterfaceIndex =
                interfaceContext.getIndex(parentInterfaceName, readContext.getMappingContext());

        //TODO stdDumpReply keys need to be filtered as egress only
        final Optional<AclInterfaceListDetailsReplyDump> stdDumpReply =
                aclReferenceDumpManager
                        .getDump(instanceIdentifier, readContext.getModificationCache(), parentInterfaceIndex);

        return getStandardAclSetKeys(readContext, stdDumpReply, false).collect(Collectors.toList());
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<AclSet> list) {
        ((AclSetsBuilder) builder).setAclSet(list);
    }

    @Nonnull
    @Override
    public AclSetBuilder getBuilder(@Nonnull final InstanceIdentifier<AclSet> instanceIdentifier) {
        return new AclSetBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<AclSet> instanceIdentifier,
                                      @Nonnull final AclSetBuilder aclSetBuilder,
                                      @Nonnull final ReadContext readContext)
            throws ReadFailedException {
        final AclSetKey vppAclsKey = instanceIdentifier.firstKeyOf(AclSet.class);
        final String aclName = vppAclsKey.getName();
        final MappingContext mappingContext = readContext.getMappingContext();
        ModificationCache modificationCache = readContext.getModificationCache();

        if (standardAclContext.containsAcl(aclName, mappingContext)) {
            parseStandardAclSet(instanceIdentifier, aclSetBuilder, aclName, mappingContext, modificationCache);
        }

    }
}
