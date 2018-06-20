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

package io.fd.hc2vpp.v3po.interfaces;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.AbstractInterfaceTypeCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.MacTranslator;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.AfPacketCreate;
import io.fd.vpp.jvpp.core.dto.AfPacketCreateReply;
import io.fd.vpp.jvpp.core.dto.AfPacketDelete;
import io.fd.vpp.jvpp.core.dto.AfPacketDeleteReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.interfaces._interface.AfPacket;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AfPacketCustomizer extends AbstractInterfaceTypeCustomizer<AfPacket>
    implements MacTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AfPacketCustomizer.class);
    private final NamingContext interfaceContext;

    public AfPacketCustomizer(@Nonnull final FutureJVppCore vppApi, @Nonnull final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev170607.AfPacket.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<AfPacket> id,
                                        @Nonnull final AfPacket dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        final int swIfIndex = createAfPacketIfc(id, swIfName, dataAfter);

        // Add new interface to our interface context
        interfaceContext.addName(swIfIndex, swIfName, writeContext.getMappingContext());
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<AfPacket> id,
                                        @Nonnull final AfPacket dataBefore, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        deleteAfPacketIfc(id, swIfName, dataBefore);

        // Remove interface from interface context
        interfaceContext.removeName(swIfName, writeContext.getMappingContext());
    }

    private int createAfPacketIfc(final InstanceIdentifier<AfPacket> id, final String swIfName, final AfPacket afPacket)
        throws WriteFailedException {
        LOG.debug("Creating af_packet interface {}: {}", swIfName, afPacket);
        final CompletionStage<AfPacketCreateReply> createAfPacketIfReplyCompletionStage =
            getFutureJVpp().afPacketCreate(getCreateRequest(afPacket));
        final AfPacketCreateReply reply =
            getReplyForCreate(createAfPacketIfReplyCompletionStage.toCompletableFuture(), id, afPacket);
        LOG.debug("Af_packet interface {} created successfully: {}", swIfName, afPacket);
        return reply.swIfIndex;
    }

    private AfPacketCreate getCreateRequest(@Nonnull final AfPacket afPacket) {
        final AfPacketCreate request = new AfPacketCreate();
        checkArgument(afPacket.getHostInterfaceName() != null,
            "host-interface-name is mandatory for af-packet interface");
        request.hostIfName = afPacket.getHostInterfaceName().getBytes(StandardCharsets.UTF_8);
        checkArgument(request.hostIfName.length <= 64,
            "Interface name for af_packet interface should not be longer than 64 bytes, but was %s",
            request.hostIfName.length);
        final PhysAddress mac = afPacket.getMac();
        if (mac == null) {
            request.useRandomHwAddr = 1;
            request.hwAddr = new byte[6];
        } else {
            request.useRandomHwAddr = 0;
            request.hwAddr = parseMac(mac.getValue());
        }
        return request;
    }

    private void deleteAfPacketIfc(final InstanceIdentifier<AfPacket> id, final String swIfName,
                                   final AfPacket afPacket) throws WriteFailedException {
        LOG.debug("Deleting af_packet interface {}: {}", swIfName, afPacket);
        final CompletionStage<AfPacketDeleteReply> deleteAfPacketIfReplyCompletionStage =
            getFutureJVpp().afPacketDelete(getDeleteRequest(afPacket));

        getReplyForDelete(deleteAfPacketIfReplyCompletionStage.toCompletableFuture(), id);
        LOG.debug("Af_packet interface {} deleted successfully: {}", swIfName, afPacket);
    }

    private AfPacketDelete getDeleteRequest(@Nonnull final AfPacket afPacket) {
        final AfPacketDelete request = new AfPacketDelete();
        request.hostIfName = afPacket.getHostInterfaceName().getBytes(StandardCharsets.UTF_8);
        return request;
    }
}
