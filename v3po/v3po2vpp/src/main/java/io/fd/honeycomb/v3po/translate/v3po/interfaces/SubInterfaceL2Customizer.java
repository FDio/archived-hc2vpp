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

import io.fd.honeycomb.v3po.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for writing vlan sub interface l2 configuration
 */
public class SubInterfaceL2Customizer extends FutureJVppCustomizer implements WriterCustomizer<L2> {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceL2Customizer.class);
    private final NamingContext interfaceContext;
    private final InterconnectionWriteUtils icWriterUtils;

    public SubInterfaceL2Customizer(final FutureJVpp vppApi, final NamingContext interfaceContext,
                                    final NamingContext bridgeDomainContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
        this.icWriterUtils = new InterconnectionWriteUtils(vppApi, interfaceContext, bridgeDomainContext);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String subInterfaceName = getSubInterfaceName(id);
        final int subInterfaceIndex = interfaceContext.getIndex(subInterfaceName, writeContext.getMappingContext());
        setL2(id, subInterfaceIndex, subInterfaceName, dataAfter, writeContext);
    }

    private String getSubInterfaceName(@Nonnull final InstanceIdentifier<L2> id) {
        final InterfaceKey parentInterfacekey = id.firstKeyOf(Interface.class);
        final SubInterfaceKey subInterfacekey = id.firstKeyOf(SubInterface.class);
        return SubInterfaceUtils
                .getSubInterfaceName(parentInterfacekey.getName(), subInterfacekey.getIdentifier().intValue());
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataBefore,
                                        @Nonnull final L2 dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        final String subInterfaceName = getSubInterfaceName(id);
        final int subInterfaceIndex = interfaceContext.getIndex(subInterfaceName, writeContext.getMappingContext());
        // TODO handle update properly (if possible)
        setL2(id, subInterfaceIndex, subInterfaceName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<L2> id, @Nonnull final L2 dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String subInterfaceName = getSubInterfaceName(id);
        final int subInterfaceIndex = interfaceContext.getIndex(subInterfaceName, writeContext.getMappingContext());
        deleteL2(id, subInterfaceIndex, subInterfaceName, dataBefore, writeContext);
    }

    private void setL2(final InstanceIdentifier<L2> id, final int swIfIndex, final String ifcName, final L2 l2,
                       final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Setting L2 for sub-interface: {}", ifcName);
        icWriterUtils.setInterconnection(id, swIfIndex, ifcName, l2.getInterconnection(), writeContext);
    }

    private void deleteL2(final InstanceIdentifier<L2> id, final int swIfIndex, final String ifcName, final L2 l2Before,
                       final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Deleting L2 for sub-interface: {}", ifcName);
        icWriterUtils.deleteInterconnection(id, swIfIndex, ifcName, l2Before.getInterconnection(), writeContext);
    }
}
