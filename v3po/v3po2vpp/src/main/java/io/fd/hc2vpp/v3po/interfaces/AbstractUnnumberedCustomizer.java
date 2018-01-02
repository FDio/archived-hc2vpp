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

package io.fd.hc2vpp.v3po.interfaces;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SwInterfaceSetUnnumbered;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.unnumbered.interfaces.rev180103.unnumbered.config.attributes.Unnumbered;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractUnnumberedCustomizer extends FutureJVppCustomizer implements WriterCustomizer<Unnumbered>,
    JvppReplyConsumer, ByteDataTranslator {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractUnnumberedCustomizer.class);

    private final NamingContext interfaceContext;

    protected AbstractUnnumberedCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                           @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    protected void setUnnumbered(@Nonnull final InstanceIdentifier<Unnumbered> id,
                                 @Nonnull final String unnumberedIfcName,
                                 @Nonnull final Unnumbered data,
                                 @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final int unnumberedIfcId = interfaceContext.getIndex(unnumberedIfcName, writeContext.getMappingContext());
        final int targetId = interfaceContext.getIndex(data.getUse(), writeContext.getMappingContext());
        final SwInterfaceSetUnnumbered request = getUnnumberedAddDelRequest(targetId, unnumberedIfcId, true);
        getReplyForWrite(getFutureJVpp().swInterfaceSetUnnumbered(request).toCompletableFuture(), id);
        LOG.debug("The {}(id={}) interface unnumbered flag was set: {}", unnumberedIfcName, unnumberedIfcId, data);
    }

    protected void disableUnnumbered(@Nonnull final InstanceIdentifier<Unnumbered> id,
                                     @Nonnull final String unnumberedIfcName,
                                     @Nonnull final Unnumbered data,
                                     @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        final int unnumberedIfcId = interfaceContext.getIndex(unnumberedIfcName, writeContext.getMappingContext());
        final int targetId = interfaceContext.getIndex(data.getUse(), writeContext.getMappingContext());
        final SwInterfaceSetUnnumbered request = getUnnumberedAddDelRequest(targetId, unnumberedIfcId, false);
        getReplyForWrite(getFutureJVpp().swInterfaceSetUnnumbered(request).toCompletableFuture(), id);
        LOG.debug("The {}(id={}) interface unnumbered flag was unset: {}", unnumberedIfcName, unnumberedIfcId, data);
    }

    private SwInterfaceSetUnnumbered getUnnumberedAddDelRequest(final int targetId, final int unnumberedIfcId,
                                                                final Boolean isAdd) {
        final SwInterfaceSetUnnumbered request = new SwInterfaceSetUnnumbered();
        request.swIfIndex = targetId;
        request.unnumberedSwIfIndex = unnumberedIfcId;
        request.isAdd = booleanToByte(isAdd);
        return request;
    }
}
