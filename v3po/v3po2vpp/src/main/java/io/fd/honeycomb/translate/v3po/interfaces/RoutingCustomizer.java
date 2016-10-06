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
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetTable;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetTableReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Routing;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingCustomizer extends FutureJVppCustomizer implements WriterCustomizer<Routing>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingCustomizer.class);
    private final NamingContext interfaceContext;

    public RoutingCustomizer(final FutureJVppCore vppApi, final NamingContext interfaceContext) {
        super(vppApi);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                       @Nonnull final Routing dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        final String ifName = id.firstKeyOf(Interface.class).getName();
        setRouting(id, ifName, dataAfter, writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                        @Nonnull final Routing dataBefore, @Nonnull final Routing dataAfter,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {

        final String ifName = id.firstKeyOf(Interface.class).getName();
        setRouting(id, ifName, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> id,
                                        @Nonnull final Routing dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        final String ifName = id.firstKeyOf(Interface.class).getName();
        disableRouting(id, ifName, writeContext);
    }

    private void setRouting(final InstanceIdentifier<Routing> id, final String name, final Routing rt,
                            final WriteContext writeContext) throws WriteFailedException {
        final int swIfc = interfaceContext.getIndex(name, writeContext.getMappingContext());
        LOG.debug("Setting routing for interface: {}, {}. Routing: {}", name, swIfc, rt);

        int vrfId = (rt != null)
                ? rt.getVrfId().intValue()
                : 0;

        if (vrfId != 0) {
            final CompletionStage<SwInterfaceSetTableReply> swInterfaceSetTableReplyCompletionStage =
                    getFutureJVpp()
                            .swInterfaceSetTable(getInterfaceSetTableRequest(swIfc, (byte) 0, /* isIpv6 */ vrfId));
            getReplyForWrite(swInterfaceSetTableReplyCompletionStage.toCompletableFuture(), id);
            LOG.debug("Routing set successfully for interface: {}, {}, routing: {}", name, swIfc, rt);
        }
    }

    /**
     * In this case, there is no such thing as delete routing,only thing that can be done is to disable it by setting
     * default value 0
     */
    private void disableRouting(final InstanceIdentifier<Routing> id, final String name,
                                final WriteContext writeContext) throws WriteFailedException {
        final int swIfc = interfaceContext.getIndex(name, writeContext.getMappingContext());
        LOG.debug("Disabling routing for interface: {}, {}.", name, swIfc);

        getReplyForDelete(getFutureJVpp()
                .swInterfaceSetTable(getInterfaceSetTableRequest(swIfc, (byte) 0, 0)).toCompletableFuture(), id);
        LOG.debug("Routing for interface: {}, {} successfully disabled", name, swIfc);

    }

    private SwInterfaceSetTable getInterfaceSetTableRequest(final int swIfc, final byte isIpv6, final int vrfId) {
        final SwInterfaceSetTable swInterfaceSetTable = new SwInterfaceSetTable();
        swInterfaceSetTable.isIpv6 = isIpv6;
        swInterfaceSetTable.swIfIndex = swIfc;
        swInterfaceSetTable.vrfId = vrfId;
        return swInterfaceSetTable;
    }
}
