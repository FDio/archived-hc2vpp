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

package io.fd.hc2vpp.acl.read;

import java.util.Optional;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.acl.util.ace.AceConverter;
import io.fd.hc2vpp.acl.util.protocol.IpProtocolReader;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.jvpp.acl.dto.AclDetails;
import io.fd.jvpp.acl.dto.AclDetailsReplyDump;
import io.fd.jvpp.acl.dto.AclDump;
import io.fd.jvpp.acl.dto.MacipAclDetails;
import io.fd.jvpp.acl.dto.MacipAclDetailsReplyDump;
import io.fd.jvpp.acl.dto.MacipAclDump;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppAclAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.rev181022.VppAclAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.AclsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AclBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.AcesBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AclCustomizer extends FutureJVppAclCustomizer
    implements InitializingListReaderCustomizer<Acl, AclKey, AclBuilder>, JvppReplyConsumer, Ipv6Translator,
    Ipv4Translator, IpProtocolReader, AceConverter {

    private static final Integer READ_ALL = -1;
    private final AclContextManager standardAclContext;
    private final AclContextManager macipAclContext;

    private final DumpCacheManager<AclDetailsReplyDump, Integer> vppAclDumpManager;
    private final DumpCacheManager<MacipAclDetailsReplyDump, Integer> macipAclDumpManager;

    public AclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                            @Nonnull final AclContextManager standardAclContext,
                            @Nonnull final AclContextManager macipAclContext) {
        super(jVppAclFacade);
        this.standardAclContext = standardAclContext;
        this.macipAclContext = macipAclContext;

        vppAclDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<AclDetailsReplyDump, Integer>()
            .withExecutor(createVppAclExecutor())
            .acceptOnly(AclDetailsReplyDump.class)
            .build();

        macipAclDumpManager = new DumpCacheManager.DumpCacheManagerBuilder<MacipAclDetailsReplyDump, Integer>()
            .withExecutor(createMacipAclExecutor())
            .acceptOnly(MacipAclDetailsReplyDump.class)
            .build();
    }

    private EntityDumpExecutor<AclDetailsReplyDump, Integer> createVppAclExecutor() {
        return (identifier, params) -> {
            AclDump request = new AclDump();
            request.aclIndex = params;
            return getReplyForRead(getjVppAclFacade().aclDump(request).toCompletableFuture(), identifier);
        };
    }

    private EntityDumpExecutor<MacipAclDetailsReplyDump, Integer> createMacipAclExecutor() {
        return (identifier, params) -> {
            MacipAclDump request = new MacipAclDump();
            request.aclIndex = params;
            return getReplyForRead(getjVppAclFacade().macipAclDump(request).toCompletableFuture(), identifier);
        };
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Acl> id,
                                                  @Nonnull final Acl readValue,
                                                  @Nonnull final ReadContext ctx) {
        return Initialized.create(id, readValue);
    }

    @Nonnull
    @Override
    public List<AclKey> getAllIds(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final ReadContext context)
        throws ReadFailedException {
        final List<AclKey> keys = new ArrayList<>();

        final Optional<AclDetailsReplyDump> vppAclDump =
            vppAclDumpManager.getDump(id, context.getModificationCache(), READ_ALL);
        if (vppAclDump.isPresent()) {
            vppAclDump.get().aclDetails.stream()
                .map(details -> standardAclContext.getAclName(details.aclIndex, context.getMappingContext()))
                    .forEach(name -> keys.add(new AclKey(name)));
        }

        final Optional<MacipAclDetailsReplyDump> macipAclDump =
            macipAclDumpManager.getDump(id, context.getModificationCache(), READ_ALL);
        if (macipAclDump.isPresent()) {
            macipAclDump.get().macipAclDetails.stream()
                .map(details -> macipAclContext.getAclName(details.aclIndex, context.getMappingContext()))
                    .forEach(name -> keys.add(new AclKey(name)));
        }

        return keys;
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Acl> readData) {
        ((AclsBuilder) builder).setAcl(readData);
    }

    @Nonnull
    @Override
    public AclBuilder getBuilder(@Nonnull final InstanceIdentifier<Acl> id) {
        return new AclBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final AclBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final AclKey key = id.firstKeyOf(Acl.class);
        builder.withKey(key);
        final String name = key.getName();

        if (standardAclContext.containsAcl(name, ctx.getMappingContext())) {
            final int index = standardAclContext.getAclIndex(name, ctx.getMappingContext());
            final Optional<AclDetailsReplyDump> dump = vppAclDumpManager.getDump(id, ctx.getModificationCache(), index);

            if (dump.isPresent() && !dump.get().aclDetails.isEmpty()) {
                final java.util.Optional<AclDetails> detail = dump.get().aclDetails.stream()
                    .filter(acl -> acl.aclIndex == index).findFirst();
                if (detail.isPresent()) {
                    final AclDetails aclDetails = detail.get();
                    setTag(builder, aclDetails.tag);
                    builder.setAces(new AcesBuilder()
                        .setAce(toStandardAces(name, aclDetails.r, standardAclContext, ctx.getMappingContext()))
                        .build());
                }
            }
        } else if (macipAclContext.containsAcl(name, ctx.getMappingContext())) {
            final int index = macipAclContext.getAclIndex(name, ctx.getMappingContext());
            final Optional<MacipAclDetailsReplyDump> dump =
                macipAclDumpManager.getDump(id, ctx.getModificationCache(), index);

            if (dump.isPresent() && !dump.get().macipAclDetails.isEmpty()) {
                final java.util.Optional<MacipAclDetails> detail =
                    dump.get().macipAclDetails.stream().filter(acl -> acl.aclIndex == index).findFirst();
                final MacipAclDetails macipAclDetails = detail.get();
                setTag(builder, macipAclDetails.tag);
                if (detail.isPresent()) {
                    builder.setAces(new AcesBuilder()
                        .setAce(toMacIpAces(name, macipAclDetails.r, macipAclContext, ctx.getMappingContext()))
                        .build());
                }
            }
        } else {
            throw new IllegalArgumentException("Unsupported acl: " + id);
        }
    }

    private void setTag(@Nonnull final AclBuilder builder, @Nullable final byte[] tag) {
        if (tag != null) {
            final String strTag = toString(tag);
            if (strTag.length() > 0) {
                builder.addAugmentation(
                    VppAclAugmentation.class, new VppAclAugmentationBuilder().setTag(strTag).build()
                );
            }
        }
    }
}
