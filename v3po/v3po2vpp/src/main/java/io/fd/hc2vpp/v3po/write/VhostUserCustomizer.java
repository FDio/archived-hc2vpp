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

package io.fd.hc2vpp.v3po.write;

import io.fd.hc2vpp.common.translate.util.AbstractInterfaceTypeCustomizer;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.CreateVhostUserIf;
import io.fd.jvpp.core.dto.CreateVhostUserIfReply;
import io.fd.jvpp.core.dto.DeleteVhostUserIf;
import io.fd.jvpp.core.dto.DeleteVhostUserIfReply;
import io.fd.jvpp.core.dto.ModifyVhostUserIf;
import io.fd.jvpp.core.dto.ModifyVhostUserIfReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VhostUserRole;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.interfaces._interface.VhostUser;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for passing vhost user interface CRD operations to VPP
 */
public class VhostUserCustomizer extends AbstractInterfaceTypeCustomizer<VhostUser>
        implements ByteDataTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(VhostUserCustomizer.class);
    private final NamingContext interfaceContext;

    public VhostUserCustomizer(@Nonnull final FutureJVppCore vppApi, @Nonnull final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190527.VhostUser.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<VhostUser> id,
                                        @Nonnull final VhostUser dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        createVhostUserIf(id, swIfName, dataAfter, writeContext);
    }

    private void createVhostUserIf(final InstanceIdentifier<VhostUser> id, final String swIfName,
                                   final VhostUser vhostUser, final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Creating vhost user interface: name={}, vhostUser={}", swIfName, vhostUser);

        final CompletionStage<CreateVhostUserIfReply> createVhostUserIfReplyCompletionStage =
                getFutureJVpp().createVhostUserIf(getCreateVhostUserIfRequest(vhostUser));
        final CreateVhostUserIfReply reply =
                getReplyForCreate(createVhostUserIfReplyCompletionStage.toCompletableFuture(), id, vhostUser);
        LOG.debug("Vhost user interface created successfully for: {}, vhostUser: {}", swIfName, vhostUser);
        // Add new interface to our interface context
        interfaceContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
    }

    private CreateVhostUserIf getCreateVhostUserIfRequest(final VhostUser vhostUser) {
        CreateVhostUserIf request = new CreateVhostUserIf();
        request.isServer = booleanToByte(VhostUserRole.Server.equals(vhostUser.getRole()));
        request.sockFilename = vhostUser.getSocket().getBytes();
        final Long deviceInstance = vhostUser.getDeviceInstance();
        if (deviceInstance == null) {
            request.renumber = 0;
        } else {
            request.renumber = 1;
            request.customDevInstance = Math.toIntExact(deviceInstance);
        }
        final String tag = vhostUser.getTag();
        if (tag != null) {
            request.tag = tag.getBytes(StandardCharsets.US_ASCII);
        }
        request.useCustomMac = 0;
        request.macAddress = new byte[]{};
        return request;
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VhostUser> id,
                                        @Nonnull final VhostUser dataBefore, @Nonnull final VhostUser dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        modifyVhostUserIf(id, swIfName, dataBefore, dataAfter, writeContext);
    }

    private void modifyVhostUserIf(final InstanceIdentifier<VhostUser> id, final String swIfName,
                                   final VhostUser vhostUserBefore, final VhostUser vhostUserAfter,
                                   final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Updating vhost user interface: name={}, vhostUser={}", swIfName, vhostUserAfter);
        final CompletionStage<ModifyVhostUserIfReply> modifyVhostUserIfReplyCompletionStage =
                getFutureJVpp()
                        .modifyVhostUserIf(getModifyVhostUserIfRequest(vhostUserAfter,
                                interfaceContext.getIndex(swIfName, writeContext.getMappingContext())));

        getReplyForUpdate(modifyVhostUserIfReplyCompletionStage.toCompletableFuture(), id, vhostUserBefore,
                vhostUserAfter);
        LOG.debug("Vhost user interface updated successfully for: {}, vhostUser: {}", swIfName, vhostUserAfter);
    }

    private ModifyVhostUserIf getModifyVhostUserIfRequest(final VhostUser vhostUser, final int swIfIndex) {
        ModifyVhostUserIf request = new ModifyVhostUserIf();
        request.isServer = booleanToByte(VhostUserRole.Server.equals(vhostUser.getRole()));
        request.sockFilename = vhostUser.getSocket().getBytes();
        final Long deviceInstance = vhostUser.getDeviceInstance();
        if (deviceInstance == null) {
            request.renumber = 0;
        } else {
            request.renumber = 1;
            request.customDevInstance = Math.toIntExact(deviceInstance);
        }
        request.swIfIndex = swIfIndex;
        return request;
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VhostUser> id,
                                        @Nonnull final VhostUser dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String swIfName = id.firstKeyOf(Interface.class).getName();
        deleteVhostUserIf(id, swIfName, dataBefore, writeContext);
    }

    private void deleteVhostUserIf(final InstanceIdentifier<VhostUser> id, final String swIfName,
                                   final VhostUser vhostUser, final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Deleting vhost user interface: name={}, vhostUser={}", swIfName, vhostUser);
        final CompletionStage<DeleteVhostUserIfReply> deleteVhostUserIfReplyCompletionStage =
                getFutureJVpp().deleteVhostUserIf(getDeleteVhostUserIfRequest(
                        interfaceContext.getIndex(swIfName, writeContext.getMappingContext())));

        getReplyForDelete(deleteVhostUserIfReplyCompletionStage.toCompletableFuture(), id);
        LOG.debug("Vhost user interface deleted successfully for: {}, vhostUser: {}", swIfName, vhostUser);
        // Remove interface from our interface context
        interfaceContext.removeName(swIfName, writeContext.getMappingContext());
    }

    private DeleteVhostUserIf getDeleteVhostUserIfRequest(final int swIfIndex) {
        DeleteVhostUserIf request = new DeleteVhostUserIf();
        request.swIfIndex = swIfIndex;
        return request;
    }
}
