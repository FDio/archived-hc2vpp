/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.mpls;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.MplsTableAddDel;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetMplsEnable;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.mpls.rev170702.interfaces.mpls.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MplsInterfaceCustomizer extends FutureJVppCustomizer
    implements ListWriterCustomizer<Interface, InterfaceKey>, JvppReplyConsumer, ByteDataTranslator {

    private static final Logger LOG = LoggerFactory.getLogger(MplsInterfaceCustomizer.class);

    private final NamingContext ifcContext;

    MplsInterfaceCustomizer(@Nonnull final FutureJVppCore vppApi, @Nonnull final NamingContext ifcContext) {
        super(vppApi);
        this.ifcContext = requireNonNull(ifcContext, "ifcContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                       @Nonnull final Interface ifc,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String swIfName = ifc.getName();
        final int swIfIndex = ifcContext.getIndex(swIfName, writeContext.getMappingContext());
        checkArgument(ifc.getConfig() != null, "MPLS interface configuration missing");
        final Boolean enabled = ifc.getConfig().isEnabled();
        LOG.debug("Configuring MPLS on interface {}(id={}): enabled={}", swIfName, swIfIndex, enabled);

        // The MPLS default table must also be explicitly created via the API before we enable it on interface
        // If table already exists, request is ignored by VPP.
        // In future, it might be useful to implement MPLS table management (HC2VPP-260).
        createDefaultMplsTable(id);

        // ietf-mpls does not define config node as mandatory child of MPLS interface list
        setInterfaceMplsState(id, swIfName, swIfIndex, enabled);

        LOG.debug("MPLS successfully configured on interface {}(id={})", swIfName, swIfIndex);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore,
                                        @Nonnull final Interface dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        writeCurrentAttributes(id, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface ifc,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String swIfName = ifc.getName();
        final int swIfIndex = ifcContext.getIndex(swIfName, writeContext.getMappingContext());
        LOG.debug("Disabling MPLS on interface {}(id={})", swIfName, swIfIndex);

        // Map delete to MPLS disable regardless of previous MPLS config:
        setInterfaceMplsState(id, swIfName, swIfIndex, false);

        LOG.debug("MPLS successfully disabled on interface {}(id={})", swIfName, swIfIndex);
    }

    private void createDefaultMplsTable(@Nonnull final InstanceIdentifier<Interface> id) throws WriteFailedException {
        final MplsTableAddDel request = new MplsTableAddDel();
        // Map delete to MPLS disable regardless of previous MPLS config:
        request.mtIsAdd = 1;
        request.mtTableId = 0;
        request.mtName = new byte[0];
        LOG.trace("Creating default MPLS table", request);
        getReplyForWrite(getFutureJVpp().mplsTableAddDel(request).toCompletableFuture(), id);

    }

    private void setInterfaceMplsState(@Nonnull final InstanceIdentifier<Interface> id, @Nonnull final String swIfName,
                                       final int swIfIndex, final boolean enabled) throws WriteFailedException {
        final SwInterfaceSetMplsEnable request = new SwInterfaceSetMplsEnable();
        // Map delete to MPLS disable regardless of previous MPLS config:
        request.enable = booleanToByte(enabled);
        request.swIfIndex = swIfIndex;
        LOG.trace("Updating MPLS flag for interface {}(id={}): {}", swIfName, swIfIndex, request);
        getReplyForWrite(getFutureJVpp().swInterfaceSetMplsEnable(request).toCompletableFuture(), id);
    }
}
