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

package io.fd.hc2vpp.policer.write;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.PolicerClassifySetInterface;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang._interface.policer.rev170315._interface.policer.attributes.Policer;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class InterfacePolicerCustomizer extends FutureJVppCustomizer implements WriterCustomizer<Policer>,
    ByteDataTranslator, JvppReplyConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(InterfacePolicerCustomizer.class);

    private final NamingContext interfaceContext;
    private final VppClassifierContextManager classifyTableContext;

    InterfacePolicerCustomizer(@Nonnull final FutureJVppCore vppApi, @Nonnull final NamingContext interfaceContext,
                               @Nonnull final VppClassifierContextManager classifyTableContext) {
        super(vppApi);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Policer> id,
                                       @Nonnull final Policer dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Applying policer id={}: {} to interface", id, dataAfter);
        assignPolicer(id, dataAfter, true, writeContext.getMappingContext());
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Policer> id,
                                        @Nonnull final Policer dataBefore, @Nonnull final Policer dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Updating policer-interface assignment id={} dataBefore={} dataAfter={}", id, dataBefore, dataAfter);
        assignPolicer(id, dataAfter, true, writeContext.getMappingContext());
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Policer> id,
                                        @Nonnull final Policer dataBefore, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Removing policer-interface assignment id={} dataBefore={}", id, dataBefore);
        assignPolicer(id, dataBefore, true, writeContext.getMappingContext());
    }

    private void assignPolicer(final InstanceIdentifier<Policer> id, final Policer policer, final boolean isAdd,
                               final MappingContext ctx) throws WriteFailedException {
        final PolicerClassifySetInterface request = new PolicerClassifySetInterface();
        request.isAdd = booleanToByte(isAdd);

        request.swIfIndex = interfaceContext.getIndex(id.firstKeyOf(Interface.class).getName(), ctx);
        request.ip4TableIndex = ~0;
        request.ip6TableIndex = ~0;
        request.l2TableIndex = ~0;
        if (policer.getL2Table() != null) {
            request.l2TableIndex = classifyTableContext.getTableIndex(policer.getL2Table(), ctx);
        }
        if (policer.getIp4Table() != null) {
            request.ip4TableIndex = classifyTableContext.getTableIndex(policer.getIp4Table(), ctx);
        }
        if (policer.getIp6Table() != null) {
            request.ip6TableIndex = classifyTableContext.getTableIndex(policer.getIp6Table(), ctx);
        }
        getReplyForWrite(getFutureJVpp().policerClassifySetInterface(request).toCompletableFuture(), id);
    }
}
