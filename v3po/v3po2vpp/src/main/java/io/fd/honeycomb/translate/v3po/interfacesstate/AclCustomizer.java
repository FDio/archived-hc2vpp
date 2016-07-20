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

package io.fd.honeycomb.translate.v3po.interfacesstate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces.state._interface.AclBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.ClassifyTableByInterface;
import org.openvpp.jvpp.core.dto.ClassifyTableByInterfaceReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for reading ACLs enabled on given interface.
 */
public class AclCustomizer extends FutureJVppCustomizer
    implements ReaderCustomizer<Acl, AclBuilder>, AclReader {

    private static final Logger LOG = LoggerFactory.getLogger(AclCustomizer.class);
    private final NamingContext interfaceContext;
    private final VppClassifierContextManager classifyTableContext;

    public AclCustomizer(@Nonnull final FutureJVppCore jvpp, @Nonnull final NamingContext interfaceContext,
                         @Nonnull final VppClassifierContextManager classifyTableContext) {
        super(jvpp);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final Acl readValue) {
        ((VppInterfaceStateAugmentationBuilder) parentBuilder).setAcl(readValue);
    }

    @Nonnull
    @Override
    public AclBuilder getBuilder(@Nonnull final InstanceIdentifier<Acl> id) {
        return new AclBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final AclBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("Reading attributes for interface ACL: {}", id);
        final InterfaceKey interfaceKey = id.firstKeyOf(Interface.class);
        checkArgument(interfaceKey != null, "No parent interface key found");

        final ClassifyTableByInterface request = new ClassifyTableByInterface();
        request.swIfIndex = interfaceContext.getIndex(interfaceKey.getName(), ctx.getMappingContext());
        try {
            final ClassifyTableByInterfaceReply reply = TranslateUtils
                .getReplyForRead(getFutureJVpp().classifyTableByInterface(request).toCompletableFuture(), id);

            builder.setL2Acl(readL2Acl(reply.l2TableId, classifyTableContext, ctx.getMappingContext()));
            builder.setIp4Acl(readIp4Acl(reply.ip4TableId, classifyTableContext, ctx.getMappingContext()));
            builder.setIp6Acl(readIp6Acl(reply.ip6TableId, classifyTableContext, ctx.getMappingContext()));

            if (LOG.isTraceEnabled()) {
                LOG.trace("Attributes for ACL {} successfully read: {}", id, builder.build());
            }
        } catch (VppBaseCallException e) {
            throw new ReadFailedException(id, e);
        }
    }
}
