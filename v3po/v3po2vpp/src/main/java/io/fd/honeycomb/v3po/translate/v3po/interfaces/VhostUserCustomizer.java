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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.fd.honeycomb.v3po.translate.v3po.util.AbstractInterfaceTypeCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUserRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.VhostUser;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.CreateVhostUserIf;
import org.openvpp.jvpp.dto.CreateVhostUserIfReply;
import org.openvpp.jvpp.dto.DeleteVhostUserIf;
import org.openvpp.jvpp.dto.DeleteVhostUserIfReply;
import org.openvpp.jvpp.dto.ModifyVhostUserIf;
import org.openvpp.jvpp.dto.ModifyVhostUserIfReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for passing vhost user interface CRD operations to VPP
 */
public class VhostUserCustomizer extends AbstractInterfaceTypeCustomizer<VhostUser> {

    private static final Logger LOG = LoggerFactory.getLogger(VhostUserCustomizer.class);
    private final NamingContext interfaceContext;

    public VhostUserCustomizer(@Nonnull final FutureJVpp vppApi, @Nonnull final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = Preconditions.checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Nonnull
    @Override
    public Optional<VhostUser> extract(@Nonnull final InstanceIdentifier<VhostUser> currentId,
                                       @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getVhostUser());
    }

    @Override
    protected Class<? extends InterfaceType> getExpectedInterfaceType() {
        return org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VhostUser.class;
    }

    @Override
    protected final void writeInterface(@Nonnull final InstanceIdentifier<VhostUser> id,
                                       @Nonnull final VhostUser dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.CreateFailedException {
        try {
            createVhostUserIf(id.firstKeyOf(Interface.class).getName(), dataAfter, writeContext);
        } catch (VppApiInvocationException | IllegalInterfaceTypeException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    private void createVhostUserIf(final String swIfName, final VhostUser vhostUser, final WriteContext writeContext) throws VppApiInvocationException {
        LOG.debug("Creating vhost user interface: name={}, vhostUser={}", swIfName, vhostUser);
        final CompletionStage<CreateVhostUserIfReply> createVhostUserIfReplyCompletionStage =
                getFutureJVpp().createVhostUserIf(getCreateVhostUserIfRequest(vhostUser));

        final CreateVhostUserIfReply reply =
                V3poUtils.getReply(createVhostUserIfReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to create vhost user interface: {}, vhostUser: {}", swIfName, vhostUser);
            throw new VppApiInvocationException("createVhostUserIf", reply.context, reply.retval);
        } else {
            LOG.debug("Vhost user interface created successfully for: {}, vhostUser: {}", swIfName, vhostUser);
            // Add new interface to our interface context
            interfaceContext.addName(reply.swIfIndex, swIfName, writeContext.getMappingContext());
        }
    }

    private CreateVhostUserIf getCreateVhostUserIfRequest(final VhostUser vhostUser) {
        CreateVhostUserIf request = new CreateVhostUserIf();
        request.isServer = V3poUtils.booleanToByte(VhostUserRole.Server.equals(vhostUser.getRole()));
        request.sockFilename = vhostUser.getSocket().getBytes();
        request.renumber = 0; // TODO
        request.customDevInstance = 0; // TODO
        request.useCustomMac = 0;
        request.macAddress = new byte[]{};
        return request;
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<VhostUser> id,
                                        @Nonnull final VhostUser dataBefore, @Nonnull final VhostUser dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException.UpdateFailedException {
        if (dataBefore.equals(dataAfter)) {
            LOG.debug("dataBefore equals dataAfter, update will not be performed");
            return;
        }

        try {
            modifyVhostUserIf(id.firstKeyOf(Interface.class).getName(), dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    private void modifyVhostUserIf(final String swIfName, final VhostUser vhostUser, final WriteContext writeContext) throws VppApiInvocationException {
        LOG.debug("Updating vhost user interface: name={}, vhostUser={}", swIfName, vhostUser);
        final CompletionStage<ModifyVhostUserIfReply> modifyVhostUserIfReplyCompletionStage =
                getFutureJVpp()
                        .modifyVhostUserIf(getModifyVhostUserIfRequest(vhostUser, interfaceContext.getIndex(swIfName, writeContext.getMappingContext())));

        final ModifyVhostUserIfReply reply =
                V3poUtils.getReply(modifyVhostUserIfReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to update vhost user interface: {}, vhostUser: {}", swIfName, vhostUser);
            throw new VppApiInvocationException("modifyVhostUserIf", reply.context, reply.retval);
        } else {
            LOG.debug("Vhost user interface updated successfully for: {}, vhostUser: {}", swIfName, vhostUser);
        }
    }

    private ModifyVhostUserIf getModifyVhostUserIfRequest(final VhostUser vhostUser, final int swIfIndex) {
        ModifyVhostUserIf request = new ModifyVhostUserIf();
        request.isServer = V3poUtils.booleanToByte(VhostUserRole.Server.equals(vhostUser.getRole()));
        request.sockFilename = vhostUser.getSocket().getBytes();
        request.renumber = 0; // TODO
        request.customDevInstance = 0; // TODO
        request.swIfIndex = swIfIndex;
        return request;
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<VhostUser> id,
                                        @Nonnull final VhostUser dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException.DeleteFailedException {
        try {
            deleteVhostUserIf(id.firstKeyOf(Interface.class).getName(), dataBefore, writeContext);
        } catch (VppApiInvocationException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void deleteVhostUserIf(final String swIfName, final VhostUser vhostUser, final WriteContext writeContext) throws VppApiInvocationException {
        LOG.debug("Deleting vhost user interface: name={}, vhostUser={}", swIfName, vhostUser);
        final CompletionStage<DeleteVhostUserIfReply> deleteVhostUserIfReplyCompletionStage =
                getFutureJVpp().deleteVhostUserIf(getDeleteVhostUserIfRequest(interfaceContext.getIndex(swIfName, writeContext.getMappingContext())));

        final DeleteVhostUserIfReply reply =
                V3poUtils.getReply(deleteVhostUserIfReplyCompletionStage.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.debug("Failed to delete vhost user interface: {}, vhostUser: {}", swIfName, vhostUser);
            throw new VppApiInvocationException("modifyVhostUserIf", reply.context, reply.retval);
        } else {
            LOG.debug("Vhost user interface deleted successfully for: {}, vhostUser: {}", swIfName, vhostUser);
            // Remove interface from our interface context
            interfaceContext.removeName(swIfName, writeContext.getMappingContext());
        }
    }

    private DeleteVhostUserIf getDeleteVhostUserIfRequest(final int swIfIndex) {
        DeleteVhostUserIf request = new DeleteVhostUserIf();
        request.swIfIndex = swIfIndex;
        return request;
    }
}
