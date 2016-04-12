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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.Context;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.VppApiInvocationException;
import io.fd.honeycomb.v3po.translate.v3po.utils.V3poUtils;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Routing;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingCustomizer extends VppApiCustomizer implements ChildWriterCustomizer<Routing> {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingCustomizer.class);

    public RoutingCustomizer(final org.openvpp.vppjapi.vppApi vppApi) {
        super(vppApi);
    }

    @Nonnull
    @Override
    public Optional<Routing> extract(@Nonnull final InstanceIdentifier<Routing> currentId,
                                     @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getRouting());
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                       @Nonnull final Routing dataAfter, @Nonnull final Context writeContext)
        throws WriteFailedException.CreateFailedException {
        final Interface ifc = (Interface) writeContext.get(InterfaceCustomizer.IFC_AFTER_CTX);

        try {
            setRouting(ifc.getName(), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of Routing failed", e);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                        @Nonnull final Routing dataBefore, @Nonnull final Routing dataAfter,
                                        @Nonnull final Context writeContext)
        throws WriteFailedException.UpdateFailedException {
        final Interface ifcBefore = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);
        final Interface ifcAfter = (Interface) writeContext.get(InterfaceCustomizer.IFC_BEFORE_CTX);

        try {
            // TODO handle updates properly
            setRouting(ifcAfter.getName(), dataAfter);
        } catch (VppApiInvocationException e) {
            LOG.warn("Update of Routing failed", e);
            throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                        @Nonnull final Routing dataBefore, @Nonnull final Context writeContext) {
        // TODO implement delete
    }

    private void setRouting(final String name, final Routing rt) throws VppApiInvocationException {
        final int swIfc = getSwIfc(name);
        LOG.debug("Setting routing for interface: {}, {}. Routing: {}", name, swIfc, rt);

        int vrfId = (rt != null)
            ? rt.getVrfId().intValue()
            : 0;

        if (vrfId != 0) {
            final int ctxId = getVppApi().swInterfaceSetTable(swIfc, (byte) 0, /* isIpv6 */ vrfId);
            final int rv = V3poUtils.waitForResponse(ctxId, getVppApi());
            if (rv < 0) {
                LOG.debug("Failed to set routing for interface: {}, {}, vxlan: {}", name, swIfc, rt);
                throw new VppApiInvocationException("swInterfaceSetTable", ctxId, rv);
            } else {
                LOG.debug("Routing set successfully for interface: {}, {}, routing: {}", name, swIfc, rt);
            }
        }
    }

    private int getSwIfc(final String name) {
        int swIfcIndex = getVppApi().swIfIndexFromName(name);
        checkArgument(swIfcIndex != -1, "Interface %s does not exist", name);
        return swIfcIndex;
    }

}
