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

package io.fd.honeycomb.translate.v3po.interfaces;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.WriteTimeoutException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.SwInterfaceSetFlags;
import org.openvpp.jvpp.core.dto.SwInterfaceSetFlagsReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ietf interface write customizer that only caches interface objects for child writers
 */
public class InterfaceCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Interface, InterfaceKey> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);
    private final NamingContext interfaceContext;

    public InterfaceCustomizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
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
        } catch (VppBaseCallException e) {
            LOG.warn("Update of VppInterfaceAugment failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final Interface dataAfter,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException {

        try {
            updateInterface(id, dataBefore, dataAfter, writeContext);
        } catch (VppBaseCallException e) {
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

    private void setInterface(final InstanceIdentifier<Interface> id, final Interface swIf,
                              final WriteContext writeContext)
        throws VppBaseCallException, WriteTimeoutException {
        LOG.debug("Setting interface: {} to: {}", id, swIf);
        setInterfaceAttributes(id, swIf, swIf.getName(), writeContext);
    }

    private void setInterfaceAttributes(final InstanceIdentifier<Interface> id, final Interface swIf,
                                        final String swIfName, final WriteContext writeContext)
        throws VppBaseCallException, WriteTimeoutException {

        setInterfaceFlags(id, swIfName, interfaceContext.getIndex(swIfName, writeContext.getMappingContext()),
            swIf.isEnabled() ? (byte) 1 : (byte) 0);
    }

    private void updateInterface(final InstanceIdentifier<Interface> id,
                                 final Interface dataBefore,
                                 final Interface dataAfter, final WriteContext writeContext)
        throws VppBaseCallException, WriteTimeoutException {
        LOG.debug("Updating interface:{} to: {}", id, dataAfter);
        setInterfaceAttributes(id, dataAfter, dataAfter.getName(), writeContext);
    }

    private void setInterfaceFlags(final InstanceIdentifier<Interface> id, final String swIfName, final int swIfIndex,
                                   final byte enabled)
        throws VppBaseCallException, WriteTimeoutException {
        final CompletionStage<SwInterfaceSetFlagsReply> swInterfaceSetFlagsReplyFuture = getFutureJVpp().swInterfaceSetFlags(
            getSwInterfaceSetFlagsInput(swIfIndex, enabled, (byte) 0 /* deleted */));

        LOG.debug("Updating interface flags for: {}, index: {}, enabled: {}", swIfName, swIfIndex, enabled);

        TranslateUtils.getReplyForWrite(swInterfaceSetFlagsReplyFuture.toCompletableFuture(), id);
        LOG.debug("Interface flags updated successfully for: {}, index: {}, enabled: {}",
                swIfName, swIfIndex, enabled);
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
