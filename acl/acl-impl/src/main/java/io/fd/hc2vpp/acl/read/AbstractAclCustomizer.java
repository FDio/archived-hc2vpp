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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.TypeAwareIdentifierCacheKeyFactory;
import io.fd.jvpp.acl.dto.AclDetailsReplyDump;
import io.fd.jvpp.acl.dto.AclDump;
import io.fd.jvpp.acl.dto.AclInterfaceListDetails;
import io.fd.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.jvpp.acl.dto.AclInterfaceListDump;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Arrays;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractAclCustomizer extends FutureJVppAclCustomizer implements JvppReplyConsumer {

    final NamingContext interfaceContext;
    final AclContextManager standardAclContext;

    final DumpCacheManager<AclInterfaceListDetailsReplyDump, Integer> aclReferenceDumpManager;
    final DumpCacheManager<AclDetailsReplyDump, Integer> aclDumpManager;

    protected AbstractAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                                    @Nonnull final NamingContext interfaceContext,
                                    @Nonnull final AclContextManager standardAclContext) {
        super(jVppAclFacade);
        this.interfaceContext = interfaceContext;
        this.standardAclContext = standardAclContext;

        aclReferenceDumpManager =
                new DumpCacheManagerBuilder<AclInterfaceListDetailsReplyDump, Integer>()
                        .withExecutor(createAclReferenceDumpExecutor())
                        // Key needs to contain interface ID to distinguish dumps between interfaces
                        .withCacheKeyFactory(
                                new TypeAwareIdentifierCacheKeyFactory(AclInterfaceListDetailsReplyDump.class,
                                        ImmutableSet.of(Interface.class)))
                        .build();

        aclDumpManager = new DumpCacheManagerBuilder<AclDetailsReplyDump, Integer>()
                .withExecutor(createAclExecutor())
                .acceptOnly(AclDetailsReplyDump.class)
                .build();
    }

    private EntityDumpExecutor<AclDetailsReplyDump, Integer> createAclExecutor() {
        return (identifier, params) -> {
            AclDump request = new AclDump();
            request.aclIndex = params;
            return getReplyForRead(getjVppAclFacade().aclDump(request).toCompletableFuture(), identifier);
        };
    }

    private EntityDumpExecutor<AclInterfaceListDetailsReplyDump, Integer> createAclReferenceDumpExecutor() {
        return (identifier, params) -> {
            AclInterfaceListDump dumpRequest = new AclInterfaceListDump();
            dumpRequest.swIfIndex = params;
            return getReplyForRead(getjVppAclFacade().aclInterfaceListDump(dumpRequest).toCompletableFuture(),
                    identifier);
        };
    }

    Stream<AclSetKey> getStandardAclSetKeys(@Nonnull final ReadContext readContext,
                                            final Optional<AclInterfaceListDetailsReplyDump> dumpReply,
                                            final boolean isIngress) {
        if (dumpReply.isPresent() && !dumpReply.get().aclInterfaceListDetails.isEmpty()) {
            // if dumpReply is present, then aclInterfaceListDetails contains single element (actually it should not be
            // dump message in vpp)
            final AclInterfaceListDetails aclDetails = dumpReply.get().aclInterfaceListDetails.get(0);

            if (isIngress) {
                return Arrays.stream(aclDetails.acls).limit(aclDetails.nInput)
                        .mapToObj(aclIndex -> standardAclContext.getAclName(aclIndex, readContext.getMappingContext()))
                        .map(AclSetKey::new);
            } else {
                return Arrays.stream(aclDetails.acls).skip(aclDetails.nInput)
                        .mapToObj(aclIndex -> standardAclContext.getAclName(aclIndex, readContext.getMappingContext()))
                        .map(AclSetKey::new);
            }
        } else {
            return Stream.empty();
        }
    }

    void parseStandardAclSet(@Nonnull final InstanceIdentifier<AclSet> instanceIdentifier,
                             @Nonnull final AclSetBuilder aclSetBuilder, final String aclName,
                             final MappingContext mappingContext, final ModificationCache modificationCache)
            throws ReadFailedException {
        final int aclIndex = standardAclContext.getAclIndex(aclName, mappingContext);

        final Optional<AclDetailsReplyDump> dumpReply =
                aclDumpManager.getDump(instanceIdentifier, modificationCache, aclIndex);

        if (dumpReply.isPresent() && !dumpReply.get().aclDetails.isEmpty()) {
            aclSetBuilder.setName(aclName);
        } else {
            throw new ReadFailedException(instanceIdentifier,
                    new IllegalArgumentException(String.format("Acl with name %s not found", aclName)));
        }
    }
}
