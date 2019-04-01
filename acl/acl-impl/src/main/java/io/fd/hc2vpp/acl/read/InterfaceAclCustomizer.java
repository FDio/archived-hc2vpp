/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

import com.google.common.collect.Streams;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.jvpp.acl.dto.AclInterfaceListDetailsReplyDump;
import io.fd.jvpp.acl.dto.AclInterfaceListDump;
import io.fd.jvpp.acl.dto.MacipAclInterfaceListDetailsReplyDump;
import io.fd.jvpp.acl.dto.MacipAclInterfaceListDump;
import io.fd.jvpp.acl.future.FutureJVppAclFacade;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.attachment.points.InterfaceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceAclCustomizer extends FutureJVppAclCustomizer implements
        InitializingListReaderCustomizer<Interface, InterfaceKey, InterfaceBuilder>, JvppReplyConsumer {
    private final NamingContext interfaceContext;

    private final DumpCacheManager<MacipAclInterfaceListDetailsReplyDump, Void> macipAclInterfaceListDumpManager;
    private final DumpCacheManager<AclInterfaceListDetailsReplyDump, Void> aclInterfaceListDumpManager;

    public InterfaceAclCustomizer(final FutureJVppAclFacade futureAclFacade, final NamingContext interfaceContext) {
        super(futureAclFacade);
        this.interfaceContext = interfaceContext;

        //list all standard ACL interfaces
        AclInterfaceListDump aclInterfaceListDump = new AclInterfaceListDump();
        aclInterfaceListDump.swIfIndex = -1;
        aclInterfaceListDumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<AclInterfaceListDetailsReplyDump, Void>()
                        .withExecutor((identifier, params) -> getReplyForRead(
                                getjVppAclFacade().aclInterfaceListDump(aclInterfaceListDump).toCompletableFuture(),
                                identifier))
                        .acceptOnly(AclInterfaceListDetailsReplyDump.class)
                        .build();

        //list all macIp ACL interfaces
        MacipAclInterfaceListDump macipAclInterfaceListDump = new MacipAclInterfaceListDump();
        macipAclInterfaceListDump.swIfIndex = 0;
        macipAclInterfaceListDumpManager =
                new DumpCacheManager.DumpCacheManagerBuilder<MacipAclInterfaceListDetailsReplyDump, Void>()
                        .withExecutor((identifier, params) -> getReplyForRead(
                                getjVppAclFacade().macipAclInterfaceListDump(macipAclInterfaceListDump)
                                        .toCompletableFuture(),
                                identifier))
                        .acceptOnly(MacipAclInterfaceListDetailsReplyDump.class)
                        .build();
    }

    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull final InstanceIdentifier<Interface> instanceIdentifier,
                                                  @Nonnull final Interface anInterface,
                                                  @Nonnull final ReadContext readContext) {
        return Initialized.create(instanceIdentifier, anInterface);
    }

    @Nonnull
    @Override
    public List<InterfaceKey> getAllIds(@Nonnull final InstanceIdentifier<Interface> instanceIdentifier,
                                        @Nonnull final ReadContext readContext) throws ReadFailedException {
        final Optional<AclInterfaceListDetailsReplyDump> stdIfcDumpReply =
                aclInterfaceListDumpManager.getDump(instanceIdentifier, readContext.getModificationCache());

        final Optional<MacipAclInterfaceListDetailsReplyDump> macIpIfcDumpReply =
                macipAclInterfaceListDumpManager.getDump(instanceIdentifier, readContext.getModificationCache());

        Stream<InterfaceKey> stdAclIfcKeys =
                stdIfcDumpReply.map(Collections::singleton).orElse(Collections.emptySet()).stream()
                .map(dump -> dump.aclInterfaceListDetails)
                .flatMap(Collection::stream)
                .filter(aclInterfaceListDetails -> aclInterfaceListDetails.acls.length != 0)
                .map(details -> getInterfaceKey(readContext, details.swIfIndex));

        Stream<InterfaceKey> macIpAclIfcKeys =
                macIpIfcDumpReply.map(Collections::singleton).orElse(Collections.emptySet()).stream()
                .map(dump -> dump.macipAclInterfaceListDetails)
                .flatMap(Collection::stream)
                .map(details -> getInterfaceKey(readContext, details.swIfIndex));
        return Streams.concat(stdAclIfcKeys, macIpAclIfcKeys).distinct().collect(Collectors.toList());
    }

    private InterfaceKey getInterfaceKey(@Nonnull final ReadContext readContext, final int swIfIndex) {
        return new InterfaceKey(interfaceContext.getName(swIfIndex, readContext.getMappingContext()));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<Interface> list) {
        ((AttachmentPointsBuilder) builder).setInterface(list);
    }

    @Nonnull
    @Override
    public InterfaceBuilder getBuilder(@Nonnull final InstanceIdentifier<Interface> instanceIdentifier) {
        return new InterfaceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> instanceIdentifier,
                                      @Nonnull final InterfaceBuilder interfaceBuilder,
                                      @Nonnull final ReadContext readContext)
            throws ReadFailedException {
        interfaceBuilder.withKey(instanceIdentifier.firstKeyOf(Interface.class));
    }
}
