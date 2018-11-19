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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.TypeAwareIdentifierCacheKeyFactory;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceGet;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceGetReply;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceListDetails;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceListDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceListDump;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.AclSetsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points._interface.acl.acl.sets.AclSetKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IngressAclCustomizer extends AbstractAclCustomizer
        implements InitializingListReaderCustomizer<AclSet, AclSetKey, AclSetBuilder>, JvppReplyConsumer {
    @VisibleForTesting
    protected static final int ACL_NOT_ASSIGNED = -1;
    private static final Logger LOG = LoggerFactory.getLogger(IngressAclCustomizer.class);

    private final AclContextManager macIpAclContext;

    private final DumpCacheManager<MacipAclDetailsReplyDump, Integer> macIpAclDumpManager;
    private final DumpCacheManager<MacipAclInterfaceGetReply, Void> interfaceMacIpAclDumpManager;
    private final DumpCacheManager<MacipAclInterfaceListDetailsReplyDump, Integer> macAclReferenceDumpManager;

    public IngressAclCustomizer(final FutureJVppAclFacade futureAclFacade, final NamingContext interfaceContext,
                                final AclContextManager standardAclContext, final AclContextManager macIpAClContext) {
        super(futureAclFacade, interfaceContext, standardAclContext);
        this.macIpAclContext = macIpAClContext;

        macAclReferenceDumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<MacipAclInterfaceListDetailsReplyDump, Integer>()
                        .withExecutor(createMacIpAclReferenceDumpExecutor())
                        // Key needs to contain interface ID to distinguish dumps between interfaces
                        .withCacheKeyFactory(
                                new TypeAwareIdentifierCacheKeyFactory(MacipAclInterfaceListDetailsReplyDump.class,
                                        ImmutableSet.of(Interface.class)))
                        .build();

        // for dumping of Mac-ip details
        macIpAclDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<MacipAclDetailsReplyDump, Integer>()
                .withExecutor(createMacIpDumpExecutor())
                .acceptOnly(MacipAclDetailsReplyDump.class)
                .build();

        // for dumping of reference on interface
        interfaceMacIpAclDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<MacipAclInterfaceGetReply, Void>()
                .withExecutor(createInterfaceMacIpDumpExecutor())
                .acceptOnly(MacipAclInterfaceGetReply.class)
                .build();
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

        //TODO stdDumpReply keys need to be filtered as ingress only
        final Optional<AclInterfaceListDetailsReplyDump> stdDumpReply =
                aclReferenceDumpManager
                        .getDump(instanceIdentifier, readContext.getModificationCache(), parentInterfaceIndex);
        final Optional<MacipAclInterfaceListDetailsReplyDump> macipDumpReply =
                macAclReferenceDumpManager
                        .getDump(instanceIdentifier, readContext.getModificationCache(), parentInterfaceIndex);

        Stream<AclSetKey> macIpAclSetKeys = getMacIpAclSetKeys(readContext, macipDumpReply);
        Stream<AclSetKey> standardAclSetKeys = getStandardAclSetKeys(readContext, stdDumpReply, true);

        return Streams.concat(standardAclSetKeys, macIpAclSetKeys).distinct().collect(Collectors.toList());
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
                                      @Nonnull final ReadContext readContext) throws ReadFailedException {
        final AclSetKey vppAclsKey = instanceIdentifier.firstKeyOf(AclSet.class);
        final String interfaceName = instanceIdentifier.firstKeyOf(Interface.class).getInterfaceId();
        final String aclName = vppAclsKey.getName();
        final MappingContext mappingContext = readContext.getMappingContext();
        ModificationCache modificationCache = readContext.getModificationCache();

        if (standardAclContext.containsAcl(aclName, mappingContext)) {
            parseStandardAclSet(instanceIdentifier, aclSetBuilder, aclName, mappingContext, modificationCache);
        } else if (macIpAclContext.containsAcl(aclName, mappingContext)) {
            parseMacIpAclSet(instanceIdentifier, aclSetBuilder, interfaceName, mappingContext, modificationCache);
        }
    }

    public void parseMacIpAclSet(@Nonnull final InstanceIdentifier<AclSet> instanceIdentifier,
                                 @Nonnull final AclSetBuilder aclSetBuilder,
                                 final String interfaceName, final MappingContext mappingContext,
                                 final ModificationCache modificationCache)
            throws ReadFailedException {
        final Optional<MacipAclInterfaceGetReply> interfacesMacIpDumpReply =
                interfaceMacIpAclDumpManager.getDump(instanceIdentifier, modificationCache);
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, mappingContext);
        MacipAclInterfaceGetReply reply = interfacesMacIpDumpReply.get();
        if (reply.acls == null || reply.acls.length == 0) {
            LOG.debug("No MacACls found for interface. Iid: {}", instanceIdentifier);
            return;
        }
        final int aclIndex = reply.acls[interfaceIndex];
        if (aclIndex != ACL_NOT_ASSIGNED) {
            final Optional<MacipAclDetailsReplyDump> macIpDumpReply =
                    macIpAclDumpManager.getDump(instanceIdentifier, modificationCache, aclIndex);

            if (macIpDumpReply.isPresent() && !macIpDumpReply.get().macipAclDetails.isEmpty()) {
                aclSetBuilder.setName(macIpAclContext.getAclName(aclIndex, mappingContext));
            } else {
                // this is invalid state(Interface in VPP will act as "deny-all" for security reasons), but generally
                // it should not happen
                throw new ReadFailedException(instanceIdentifier,
                        new IllegalStateException(String.format("ACE with index %s not found in VPP", aclIndex)));
            }
        }
    }

    private EntityDumpExecutor<MacipAclInterfaceListDetailsReplyDump, Integer> createMacIpAclReferenceDumpExecutor() {
        return (identifier, params) -> {
            MacipAclInterfaceListDump dumpRequest = new MacipAclInterfaceListDump();
            dumpRequest.swIfIndex = params;
            return getReplyForRead(getjVppAclFacade().macipAclInterfaceListDump(dumpRequest).toCompletableFuture(),
                    identifier);
        };
    }

    private EntityDumpExecutor<MacipAclDetailsReplyDump, Integer> createMacIpDumpExecutor() {
        return (identifier, params) -> {
            MacipAclDump request = new MacipAclDump();
            request.aclIndex = params;

            return getReplyForRead(getjVppAclFacade().macipAclDump(request).toCompletableFuture(), identifier);
        };
    }

    private EntityDumpExecutor<MacipAclInterfaceGetReply, Void> createInterfaceMacIpDumpExecutor() {
        return (identifier, params) -> getReplyForRead(
                getjVppAclFacade().macipAclInterfaceGet(new MacipAclInterfaceGet()).toCompletableFuture(),
                identifier);
    }


    private Stream<AclSetKey> getMacIpAclSetKeys(@Nonnull final ReadContext readContext,
                                                 final Optional<MacipAclInterfaceListDetailsReplyDump> dumpReply) {
        if (dumpReply.isPresent() && !dumpReply.get().macipAclInterfaceListDetails.isEmpty()) {
            // if dumpReply is present, then aclInterfaceListDetails contains single element (actually it should not be
            // dump message in vpp)
            final MacipAclInterfaceListDetails aclDetails = dumpReply.get().macipAclInterfaceListDetails.get(0);

            return Arrays.stream(aclDetails.acls).limit(aclDetails.count)
                    .mapToObj(aclIndex -> macIpAclContext.getAclName(aclIndex, readContext.getMappingContext()))
                    .map(AclSetKey::new);
        } else {
            return Stream.empty();
        }
    }
}
