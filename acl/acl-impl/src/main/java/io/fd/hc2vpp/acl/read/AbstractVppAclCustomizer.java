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
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.TypeAwareIdentifierCacheKeyFactory;
import io.fd.vpp.jvpp.acl.dto.AclDetails;
import io.fd.vpp.jvpp.acl.dto.AclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.AclDump;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetails;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDump;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.VppAclInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppAcl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

abstract class AbstractVppAclCustomizer extends FutureJVppAclCustomizer
    implements InitializingListReaderCustomizer<VppAcls, VppAclsKey, VppAclsBuilder>, JvppReplyConsumer,
    ByteDataTranslator {

    private final NamingContext interfaceContext;
    private final NamingContext standardAclContext;

    private final DumpCacheManager<AclInterfaceListDetailsReplyDump, Integer> aclReferenceDumpManager;
    private final DumpCacheManager<AclDetailsReplyDump, Integer> aclDumpManager;

    protected AbstractVppAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                                       @Nonnull final NamingContext interfaceContext,
                                       @Nonnull final NamingContext standardAclContext) {
        super(jVppAclFacade);
        this.interfaceContext = interfaceContext;
        this.standardAclContext = standardAclContext;

        aclReferenceDumpManager =
            new DumpCacheManagerBuilder<AclInterfaceListDetailsReplyDump, Integer>()
                .withExecutor(createAclReferenceDumpExecutor())
                // Key needs to contain interface ID to distinguish dumps between interfaces
                .withCacheKeyFactory(new TypeAwareIdentifierCacheKeyFactory(AclInterfaceListDetailsReplyDump.class,
                    ImmutableSet.of(Interface.class)))
                .build();

        aclDumpManager = new DumpCacheManagerBuilder<AclDetailsReplyDump, Integer>()
            .withExecutor(createAclExecutor())
            .acceptOnly(AclDetailsReplyDump.class)
            .build();
    }

    protected static InstanceIdentifier<Acl> getAclCfgId(
        final InstanceIdentifier<Acl> id) {
        return InstanceIdentifier.create(Interfaces.class).child(
            org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class,
            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey(
                id.firstKeyOf(Interface.class).getName())).augmentation(VppAclInterfaceAugmentation.class)
            .child(Acl.class);
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

    @Nonnull
    @Override
    public final List<VppAclsKey> getAllIds(@Nonnull final InstanceIdentifier<VppAcls> id,
                                            @Nonnull final ReadContext context)
        throws ReadFailedException {

        final String parentInterfaceName = id.firstKeyOf(Interface.class).getName();
        final int parentInterfaceIndex = interfaceContext.getIndex(parentInterfaceName, context.getMappingContext());

        final Optional<AclInterfaceListDetailsReplyDump> dumpReply =
            aclReferenceDumpManager.getDump(id, context.getModificationCache(), parentInterfaceIndex);

        if (dumpReply.isPresent() && !dumpReply.get().aclInterfaceListDetails.isEmpty()) {
            // if dumpReply is present, then aclInterfaceListDetails contains single element (actually it should not be
            // dump message in vpp)
            final AclInterfaceListDetails aclDetails = dumpReply.get().aclInterfaceListDetails.get(0);
            return filterAcls(aclDetails)
                .mapToObj(aclIndex -> standardAclContext.getName(aclIndex, context.getMappingContext()))
                .map(aclName -> new VppAclsKey(aclName, VppAcl.class))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Streams ids of ACLs.
     *
     * @param aclDetails describes ACLs assigned to interface
     * @return sequence of acl ids
     */
    protected abstract IntStream filterAcls(@Nonnull final AclInterfaceListDetails aclDetails);

    @Nonnull
    @Override
    public final VppAclsBuilder getBuilder(@Nonnull final InstanceIdentifier<VppAcls> id) {
        return new VppAclsBuilder();
    }

    @Override
    public final void readCurrentAttributes(@Nonnull final InstanceIdentifier<VppAcls> id,
                                            @Nonnull final VppAclsBuilder builder,
                                            @Nonnull final ReadContext ctx) throws ReadFailedException {
        final VppAclsKey vppAclsKey = id.firstKeyOf(VppAcls.class);
        final String aclName = vppAclsKey.getName();
        final int aclIndex = standardAclContext.getIndex(aclName, ctx.getMappingContext());

        final Optional<AclDetailsReplyDump> dumpReply =
            aclDumpManager.getDump(id, ctx.getModificationCache(), aclIndex);

        if (dumpReply.isPresent() && !dumpReply.get().aclDetails.isEmpty()) {
            // FIXME (model expects hex string, but tag is written and read as ascii string)
            // decide how tag should be handled (model change might be needed).
            builder.setName(aclName);
            builder.setType(vppAclsKey.getType());
            final AclDetails aclDetails = dumpReply.get().aclDetails.get(0);
            if (aclDetails.tag != null && aclDetails.tag.length > 0) {
                builder.setTag(new HexString(printHexBinary(aclDetails.tag)));
            }
        } else {
            throw new ReadFailedException(id,
                new IllegalArgumentException(String.format("Acl with name %s not found", aclName)));
        }
    }

    @Nonnull
    @Override
    public Initialized<VppAcls> init(@Nonnull final InstanceIdentifier<VppAcls> id,
                                     @Nonnull final VppAcls vppAcls,
                                     @Nonnull final ReadContext readContext) {
        return Initialized.create(getCfgId(id), vppAcls);
    }

    protected abstract InstanceIdentifier<VppAcls> getCfgId(final InstanceIdentifier<VppAcls> id);
}
