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

import static io.fd.hc2vpp.acl.read.AbstractVppAclCustomizer.getAclCfgId;
import static io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor.NO_PARAMS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingReaderCustomizer;
import io.fd.honeycomb.translate.util.RWUtils;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.acl.dto.MacipAclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclDump;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceGet;
import io.fd.vpp.jvpp.acl.dto.MacipAclInterfaceGetReply;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.Acl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAcl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.macip.acls.base.attributes.VppMacipAclBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VppMacIpAclCustomizer extends FutureJVppAclCustomizer
    implements InitializingReaderCustomizer<VppMacipAcl, VppMacipAclBuilder>, JvppReplyConsumer, ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(VppMacIpAclCustomizer.class);
    @VisibleForTesting
    protected static final int ACL_NOT_ASSIGNED = -1;

    private final DumpCacheManager<MacipAclDetailsReplyDump, Integer> macIpAclDumpManager;
    private final DumpCacheManager<MacipAclInterfaceGetReply, Void> interfaceMacIpAclDumpManager;
    private final NamingContext interfaceContext;
    private final AclContextManager macIpAclContext;

    public VppMacIpAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                                 @Nonnull final NamingContext interfaceContext,
                                 @Nonnull final AclContextManager macIpAclContext) {
        super(jVppAclFacade);

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
        this.interfaceContext = interfaceContext;
        this.macIpAclContext = macIpAclContext;
    }

    private static InstanceIdentifier<VppMacipAcl> getCfgId(
        final InstanceIdentifier<VppMacipAcl> id) {
        return getAclCfgId(RWUtils.cutId(id, Acl.class)).child(Ingress.class).child(VppMacipAcl.class);
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

    @Nonnull
    @Override
    public VppMacipAclBuilder getBuilder(@Nonnull final InstanceIdentifier<VppMacipAcl> id) {
        return new VppMacipAclBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VppMacipAcl> id,
                                      @Nonnull final VppMacipAclBuilder builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        final String interfaceName = id.firstKeyOf(Interface.class).getName();
        final MappingContext mappingContext = ctx.getMappingContext();
        final int interfaceIndex = interfaceContext.getIndex(interfaceName, mappingContext);
        final ModificationCache modificationCache = ctx.getModificationCache();
        final Optional<MacipAclInterfaceGetReply> interfacesMacIpDumpReply =
            interfaceMacIpAclDumpManager.getDump(id, modificationCache, NO_PARAMS);

        if (interfacesMacIpDumpReply.isPresent() && interfaceIndex < interfacesMacIpDumpReply.get().count) {
            final int aclIndex = interfacesMacIpDumpReply.get().acls[interfaceIndex];
            if (aclIndex != ACL_NOT_ASSIGNED) {
                final Optional<MacipAclDetailsReplyDump> macIpDumpReply =
                    macIpAclDumpManager.getDump(id, modificationCache, aclIndex);

                if (macIpDumpReply.isPresent() && !macIpDumpReply.get().macipAclDetails.isEmpty()) {
                    builder.setName(macIpAclContext.getAclName(aclIndex, mappingContext));
                    builder.setType(
                        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppMacipAcl.class);
                    return;
                } else {
                    // this is invalid state(Interface in VPP will act as "deny-all" for security reasons), but generally
                    // it should not happen
                    throw new ReadFailedException(id,
                        new IllegalStateException(String.format("ACE with index %s not found in VPP", aclIndex)));
                }
            }
        }
        // this is valid state, so just logging
        LOG.debug("No Mac-ip ACL specified for Interface name={},index={}", interfaceName, interfaceIndex);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final VppMacipAcl readValue) {
        IngressBuilder.class.cast(parentBuilder).setVppMacipAcl(readValue);
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<VppMacipAcl> id,
                                                  @Nonnull final VppMacipAcl readValue,
                                                  @Nonnull final ReadContext ctx) {
        return Initialized.create(getCfgId(id), readValue);
    }
}
