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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.L2;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2Customizer extends FutureJVppCustomizer implements WriterCustomizer<L2> {

    private static final Logger LOG = LoggerFactory.getLogger(L2Customizer.class);
    private final NamingContext interfaceContext;
    private final InterconnectionWriteUtils icWriteUtils;

    public L2Customizer(final FutureJVpp vppApi, final NamingContext interfaceContext,
                        final NamingContext bridgeDomainContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
        this.icWriteUtils = new InterconnectionWriteUtils(vppApi, interfaceContext, bridgeDomainContext);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataAfter,
                                       @Nonnull final WriteContext writeContext)
        throws WriteFailedException {

        final String ifcName = id.firstKeyOf(Interface.class).getName();
        final int swIfc = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        setL2(id, swIfc, ifcName, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataBefore,
                                        @Nonnull final L2 dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {

        final String ifcName = id.firstKeyOf(Interface.class).getName();
        final int swIfc = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        // TODO handle update properly (if possible)
        setL2(id, swIfc, ifcName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String ifcName = id.firstKeyOf(Interface.class).getName();
        final int swIfc = interfaceContext.getIndex(ifcName, writeContext.getMappingContext());
        deleteL2(id, swIfc, ifcName, dataBefore, writeContext);
    }

    private void setL2(final InstanceIdentifier<L2> id, final int swIfIndex, final String ifcName, final L2 l2,
                       final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Setting L2 for interface: {}", ifcName);
        // Nothing besides interconnection here
        icWriteUtils.setInterconnection(id, swIfIndex, ifcName, l2.getInterconnection(), writeContext);
    }

    private void deleteL2(final InstanceIdentifier<L2> id, final int swIfIndex, final String ifcName, final L2 l2Before,
                       final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Deleting L2 for interface: {}", ifcName);
        // Nothing besides interconnection here
        icWriteUtils.deleteInterconnection(id, swIfIndex, ifcName, l2Before.getInterconnection(), writeContext);
    }
}
