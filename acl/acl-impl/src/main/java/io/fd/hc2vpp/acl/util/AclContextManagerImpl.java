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

package io.fd.hc2vpp.acl.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.util.RWUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.VppAclContextAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.VppAclMappings;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.vpp.acl.mappings.VppAclContext;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.vpp.acl.mappings.VppAclContextKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.vpp.acl.mappings.vpp.acl.context.AclMapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.vpp.acl.mappings.vpp.acl.context.AclMappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.vpp.acl.mappings.vpp.acl.context.AclMappingKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.vpp.acl.mappings.vpp.acl.context.acl.mapping.AceMapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.vpp.acl.mappings.vpp.acl.context.acl.mapping.AceMappingBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.acl.context.rev170104.vpp.acl.context.attributes.vpp.acl.mappings.vpp.acl.context.acl.mapping.AceMappingKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.acl.aces.Ace;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

/**
 * Facade on top of {@link MappingContext} that manages {@link VppAclContext}.
 */
@ThreadSafe
public final class AclContextManagerImpl implements AclContextManager {

    private static final Collector<AclMapping, ?, AclMapping> SINGLE_ITEM_ACL_COLLECTOR =
        RWUtils.singleItemCollector();

    private static final Collector<AceMapping, ?, AceMapping> SINGLE_ITEM_ACE_COLLECTOR =
        RWUtils.singleItemCollector();

    private final InstanceIdentifier<VppAclContext> ctxIid;

    private final String artificialNamePrefix;

    public AclContextManagerImpl(@Nonnull final String artificialNamePrefix, @Nonnull final String aclContextName) {
        this.artificialNamePrefix = requireNonNull(artificialNamePrefix, "artificialNamePrefix should not be null");
        requireNonNull(aclContextName, "aclContextName should not be null");
        this.ctxIid = InstanceIdentifier.create(Contexts.class)
            .augmentation(VppAclContextAugmentation.class).child(VppAclMappings.class)
            .child(VppAclContext.class, new VppAclContextKey(aclContextName));
    }

    @Override
    public synchronized void addAcl(final int id, @Nonnull final String name, @Nonnull final List<Ace> aces,
                                    @Nonnull final MappingContext ctx) {
        final KeyedInstanceIdentifier<AclMapping, AclMappingKey> mappingIid = getAclIid(name);
        final AclMappingBuilder aclMapping = new AclMappingBuilder().setIndex(id).setName(name);

        final List<AceMapping> mappings = new ArrayList<>(aces.size());
        int aceIndex = 0;
        for (final Ace ace : aces) {
            mappings.add(new AceMappingBuilder().setName(ace.getName()).setIndex(aceIndex++).build());
        }
        aclMapping.setAceMapping(mappings);
        ctx.put(mappingIid, aclMapping.build());
    }

    @Override
    public synchronized boolean containsAcl(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        final Optional<AclMapping> read = ctx.read(getAclIid(name));
        return read.isPresent();
    }

    @Override
    public synchronized int getAclIndex(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        final Optional<AclMapping> read = ctx.read(getAclIid(name));
        checkArgument(read.isPresent(), "No mapping stored for name: %s", name);
        return read.get().getIndex();
    }

    @Override
    public synchronized String getAclName(final int id, @Nonnull final MappingContext ctx) {
        if (!containsAclName(id, ctx)) {
            final String artificialName = getArtificialAclName(id);
            addAcl(id, artificialName, Collections.emptyList(), ctx);
        }

        final Optional<VppAclContext> read = ctx.read(ctxIid);
        checkState(read.isPresent(), "VppAclContext for index: %s is not present. But should be", id);

        return read.get().getAclMapping().stream()
            .filter(t -> t.getIndex().equals(id))
            .collect(SINGLE_ITEM_ACL_COLLECTOR).getName();
    }

    private boolean containsAclName(final int id, @Nonnull final MappingContext mappingContext) {
        final Optional<VppAclContext> read = mappingContext.read(ctxIid);
        return read.isPresent() && read.get().getAclMapping().stream().anyMatch(t -> t.getIndex().equals(id));
    }

    private String getArtificialAclName(final int index) {
        return artificialNamePrefix + index;
    }

    @Override
    public synchronized void removeAcl(@Nonnull final String name, @Nonnull final MappingContext ctx) {
        ctx.delete(getAclIid(name));
    }

    @Override
    public synchronized String getAceName(@Nonnull final String aclName, final int aceIndex,
                                          @Nonnull final MappingContext ctx) {
        if (!containsAceName(aclName, aceIndex, ctx)) {
            final String artificialName = getArtificialAceName(aceIndex);
            addAce(aclName, aceIndex, artificialName, ctx);
        }

        final Optional<AclMapping> read = ctx.read(getAclIid(aclName));
        checkState(read.isPresent(), "AclMapping for name: %s is not present. But should be", aclName);

        return read.get().getAceMapping().stream()
            .filter(t -> t.getIndex().equals(aceIndex))
            .collect(SINGLE_ITEM_ACE_COLLECTOR).getName();
    }

    private boolean containsAceName(@Nonnull final String aclName, final int id, @Nonnull final MappingContext ctx) {
        final Optional<AclMapping> read = ctx.read(getAclIid(aclName));
        return read.isPresent() && read.get().getAceMapping().stream().anyMatch(t -> t.getIndex().equals(id));
    }

    private String getArtificialAceName(final int index) {
        return artificialNamePrefix + "rule" + index;
    }

    private KeyedInstanceIdentifier<AclMapping, AclMappingKey> getAclIid(@Nonnull final String name) {
        return ctxIid.child(AclMapping.class, new AclMappingKey(name));
    }

    private void addAce(@Nonnull final String aclName, final int aceIndex, @Nonnull final String aceName,
                        @Nonnull final MappingContext ctx) {
        final AceMappingBuilder aceMapping = new AceMappingBuilder();
        aceMapping.setIndex(aceIndex);
        aceMapping.setName(aceName);
        final KeyedInstanceIdentifier<AceMapping, AceMappingKey> iid =
            getAclIid(aclName).child(AceMapping.class, new AceMappingKey(aceName));
        ctx.put(iid, aceMapping.build());
    }
}
