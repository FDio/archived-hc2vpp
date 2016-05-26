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

import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.dto.SwInterfaceSetFlags;
import org.openvpp.jvpp.dto.SwInterfaceSetFlagsReply;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ietf interface write customizer that only caches interface objects for child writers
 */
public class InterfaceCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Interface, InterfaceKey> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);
    private final NamingContext interfaceContext;

    public InterfaceCustomizer(final FutureJVpp vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                       @Nonnull final Interface dataAfter,
                                       @Nonnull final WriteContext writeContext)
        throws WriteFailedException {

        try {
            setInterface(id, dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of VppInterfaceAugment failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final Interface dataAfter,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException.UpdateFailedException {

        try {
            updateInterface(id, dataBefore, dataAfter, writeContext);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of VppInterfaceAugment failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final WriteContext writeContext) {

        // TODO Handle deletes
    }

    @Nonnull
    @Override
    public List<Interface> extract(@Nonnull final InstanceIdentifier<Interface> currentId,
                                   @Nonnull final DataObject parentData) {
        return ((Interfaces) parentData).getInterface();
    }


    private void setInterface(final InstanceIdentifier<Interface> id, final Interface swIf,
                              final WriteContext writeContext)
        throws VppApiInvocationException, WriteFailedException {
        LOG.debug("Setting interface: {} to: {}", id, swIf);
        setInterfaceAttributes(swIf, swIf.getName(), writeContext);
    }

    private void setInterfaceAttributes(final Interface swIf, final String swIfName, final WriteContext writeContext)
        throws VppApiInvocationException {

        setInterfaceFlags(swIfName, interfaceContext.getIndex(swIfName, writeContext.getMappingContext()),
            swIf.isEnabled() ? (byte) 1 : (byte) 0);
    }

    private void updateInterface(final InstanceIdentifier<Interface> id,
                                 final Interface dataBefore,
                                 final Interface dataAfter, final WriteContext writeContext) throws VppApiInvocationException {
        LOG.debug("Updating interface:{} to: {}", id, dataAfter);
        setInterfaceAttributes(dataAfter, dataAfter.getName(), writeContext);
    }

    private void setInterfaceFlags(final String swIfName, final int swIfIndex, final byte enabled)
        throws VppApiInvocationException {
        final CompletionStage<SwInterfaceSetFlagsReply> swInterfaceSetFlagsReplyFuture = getFutureJVpp().swInterfaceSetFlags(
            getSwInterfaceSetFlagsInput(swIfIndex, enabled, (byte) 0 /* deleted */));

        LOG.debug("Updating interface flags for: {}, index: {}, enabled: {}", swIfName, swIfIndex, enabled);

        SwInterfaceSetFlagsReply reply = TranslateUtils.getReply(swInterfaceSetFlagsReplyFuture.toCompletableFuture());
        if (reply.retval < 0) {
            LOG.warn("Failed to update interface flags for: {}, index: {}, enabled: {}", swIfName, swIfIndex,
                enabled);
            throw new VppApiInvocationException("swInterfaceSetFlags", reply.context, reply.retval);
        } else {
            LOG.debug("Interface flags updated successfully for: {}, index: {}, enabled: {}, ctxId: {}",
                swIfName, swIfIndex, enabled, reply.context);
        }
    }

    private SwInterfaceSetFlags getSwInterfaceSetFlagsInput(final int swIfIndex, final byte enabled, final byte deleted) {
        final SwInterfaceSetFlags swInterfaceSetFlags = new SwInterfaceSetFlags();
        swInterfaceSetFlags.swIfIndex = swIfIndex;
        swInterfaceSetFlags.adminUpDown = enabled;
        swInterfaceSetFlags.linkUpDown = enabled;
        swInterfaceSetFlags.deleted = deleted;
        return swInterfaceSetFlags;
    }
}
