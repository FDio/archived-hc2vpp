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

import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.vppjapi.vppApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ietf interface write customizer that only caches interface objects for child writers
 */
public class InterfaceCustomizer extends VppApiCustomizer implements ListWriterCustomizer<Interface, InterfaceKey> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceCustomizer.class);

    public InterfaceCustomizer(final vppApi vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                       @Nonnull final Interface dataAfter,
                                       @Nonnull final Context writeContext)
        throws WriteFailedException {

        try {
            setInterface(id, dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of VppInterfaceAugment failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final Interface dataAfter,
                                        @Nonnull final Context writeContext)
        throws WriteFailedException.UpdateFailedException {

        try {
            updateInterface(id, dataBefore, dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of VppInterfaceAugment failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final Context writeContext) {

        // TODO Handle deletes
    }

    @Nonnull
    @Override
    public List<Interface> extract(@Nonnull final InstanceIdentifier<Interface> currentId,
                                   @Nonnull final DataObject parentData) {
        return ((Interfaces) parentData).getInterface();
    }


    private void setInterface(final InstanceIdentifier<Interface> id, final Interface swIf)
        throws VppApiInvocationException, WriteFailedException {
        LOG.info("Setting interface {}, type: {}", swIf.getName(), swIf.getType().getSimpleName());
        LOG.debug("Setting interface {}", swIf);

        String swIfName = swIf.getName();
        int swIfIndex = getVppApi().swIfIndexFromName(swIfName);

        setInterfaceAttributes(swIf, swIfName);
    }

    private void setInterfaceAttributes(final Interface swIf, final String swIfName)
        throws VppApiInvocationException {
        LOG.debug("Creating {} interface {}", swIf.getType().getSimpleName(), swIf.getName());

        setInterfaceFlags(swIfName, getVppApi().swIfIndexFromName(swIfName),
            swIf.isEnabled() ? (byte) 1 : (byte) 0);

        setDescription(swIf);
    }

    private void updateInterface(final InstanceIdentifier<Interface> id,
                                 final Interface dataBefore,
                                 final Interface dataAfter) throws VppApiInvocationException {
        LOG.info("Updating interface {}, type: {}", dataAfter.getName(), dataAfter.getType().getSimpleName());
        LOG.debug("Updating interface {}", dataAfter);

        String swIfName = dataAfter.getName();
        int swIfIndex = getVppApi().swIfIndexFromName(swIfName);

        setInterfaceAttributes(dataAfter, swIfName);
    }

    private void setInterfaceFlags(final String swIfName, final int swIfIndex, final byte enabled)
        throws VppApiInvocationException {
        int ctxId = getVppApi().swInterfaceSetFlags(swIfIndex, enabled, enabled, (byte) 0 /* deleted */);

        LOG.debug("Updating interface flags for: {}, index: {}, enabled: {}, ctxId: {}", swIfName, swIfIndex,
            enabled, ctxId);

        final int rv = V3poUtils.waitForResponse(ctxId, getVppApi());
        if (rv < 0) {
            LOG.warn("Failed to update interface flags for: {}, index: {}, enabled: {}, ctxId: {}", swIfName, swIfIndex,
                enabled, ctxId);
            throw new VppApiInvocationException("swInterfaceSetFlags", ctxId, rv);
        } else {
            LOG.debug("Interface flags updated successfully for: {}, index: {}, enabled: {}, ctxId: {}",
                swIfName, swIfIndex, enabled, ctxId);
        }
    }

    private void setDescription(final Interface swIf) {
        if (swIf.getDescription() != null) {
            getVppApi().setInterfaceDescription(swIf.getName(), swIf.getDescription());
        } else {
            getVppApi().setInterfaceDescription(swIf.getName(), "");
        }
    }
}
