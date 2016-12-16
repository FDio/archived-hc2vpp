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
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager.DumpCacheManagerBuilder;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpPostProcessingFunction;
import io.fd.vpp.jvpp.acl.dto.AclDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.AclDump;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.vpp.jvpp.acl.dto.AclInterfaceListDump;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.HexString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.EgressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214._interface.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAcls;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.acl.rev161214.vpp.acls.base.attributes.VppAclsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.VppAcl;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppAclCustomizer extends FutureJVppAclCustomizer
        implements ListReaderCustomizer<VppAcls, VppAclsKey, VppAclsBuilder>, JvppReplyConsumer, ByteDataTranslator {

    private final NamingContext interfaceContext;
    private final NamingContext standardAclContext;
    /**
     * true == ingress
     * false == egress
     */
    private final boolean input;
    private final DumpCacheManager<AclInterfaceListDetailsReplyDump, Integer> aclReferenceDumpManager;
    private final DumpCacheManager<AclDetailsReplyDump, Integer> aclDumpManager;

    public VppAclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                            @Nonnull final NamingContext interfaceContext,
                            @Nonnull final NamingContext standardAclContext,
                            final boolean input) {
        super(jVppAclFacade);
        this.interfaceContext = interfaceContext;
        this.standardAclContext = standardAclContext;
        this.input = input;

        aclReferenceDumpManager =
                new DumpCacheManagerBuilder<AclInterfaceListDetailsReplyDump, Integer>()
                        .withExecutor(createAclReferenceDumpExecutor())
                        .withPostProcessingFunction(input
                                ? createInputAclFilter()
                                : createOutputAclFilter())
                        .acceptOnly(AclInterfaceListDetailsReplyDump.class)
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

    private EntityDumpPostProcessingFunction<AclInterfaceListDetailsReplyDump> createInputAclFilter() {
        return dump -> {
            // filters acl's to first N(those are input ones)
            dump.aclInterfaceListDetails = dump.aclInterfaceListDetails
                    .stream()
                    .map(iface -> {
                        iface.acls = Arrays.copyOfRange(iface.acls, 0, iface.nInput - 1);
                        return iface;
                    })
                    .collect(Collectors.toList());
            return dump;
        };
    }

    private EntityDumpPostProcessingFunction<AclInterfaceListDetailsReplyDump> createOutputAclFilter() {
        return dump -> {
            // filters acl's to last N(those are output ones)
            dump.aclInterfaceListDetails = dump.aclInterfaceListDetails
                    .stream()
                    .map(iface -> {
                        iface.acls = Arrays.copyOfRange(iface.acls, iface.nInput, iface.acls.length);
                        return iface;
                    })
                    .collect(Collectors.toList());
            return dump;
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
    public List<VppAclsKey> getAllIds(@Nonnull final InstanceIdentifier<VppAcls> id, @Nonnull final ReadContext context)
            throws ReadFailedException {

        final String parentInterfaceName = id.firstKeyOf(Interface.class).getName();
        final int parentInterfaceIndex = interfaceContext.getIndex(parentInterfaceName, context.getMappingContext());

        final Optional<AclInterfaceListDetailsReplyDump> dumpReply =
                aclReferenceDumpManager.getDump(id, context.getModificationCache(), parentInterfaceIndex);

        if (dumpReply.isPresent() && !dumpReply.get().aclInterfaceListDetails.isEmpty()) {
            return Arrays.stream(dumpReply.get().aclInterfaceListDetails.get(0).acls)
                    .mapToObj(aclIndex -> standardAclContext.getName(aclIndex, context.getMappingContext()))
                    .map(aclName -> new VppAclsKey(aclName, VppAcl.class))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<VppAcls> readData) {
        if (input) {
            IngressBuilder.class.cast(builder).setVppAcls(readData);
        } else {
            EgressBuilder.class.cast(builder).setVppAcls(readData);
        }
    }

    @Nonnull
    @Override
    public VppAclsBuilder getBuilder(@Nonnull final InstanceIdentifier<VppAcls> id) {
        return new VppAclsBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<VppAcls> id,
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
            builder.setTag(new HexString(printHexBinary(dumpReply.get().aclDetails.get(0).tag)));
        } else {
            throw new ReadFailedException(id,
                    new IllegalArgumentException(String.format("Acl with name %s not found", aclName)));
        }
    }
}
