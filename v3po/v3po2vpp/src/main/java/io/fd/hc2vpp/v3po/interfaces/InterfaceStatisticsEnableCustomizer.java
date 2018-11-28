/*
 * Copyright (c) 2019 PANTHEON.tech.
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

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsDumpManager;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.WantPerInterfaceCombinedStats;
import io.fd.vpp.jvpp.core.dto.WantPerInterfaceCombinedStatsReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.interfaces._interface.StatisticsCollection;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterfaceStatisticsEnableCustomizer extends FutureJVppCustomizer implements
        WriterCustomizer<StatisticsCollection>, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceStatisticsEnableCustomizer.class);

    private NamingContext ifcNamingContext;
    private InterfaceCacheStatisticsDumpManager ifcStatisticsManager;

    public InterfaceStatisticsEnableCustomizer(final FutureJVppCore jvpp, final NamingContext ifcNamingContext,
                                               final InterfaceCacheStatisticsDumpManager ifcStatisticsManager) {
        super(jvpp);
        this.ifcNamingContext = ifcNamingContext;
        this.ifcStatisticsManager = ifcStatisticsManager;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<StatisticsCollection> id,
                                       @Nonnull final StatisticsCollection dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {

        InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = ifcNamingContext.getIndex(key.getName(), writeContext.getMappingContext());
        if (!dataAfter.isStatisticsEnabled()) {
            ifcStatisticsManager.disableInterface(index);
        } else {
            ifcStatisticsManager.enableInterface(index);
        }
        enableDisableStatisticsNotifications(index, dataAfter.isStatisticsEnabled().booleanValue());
    }

    private void enableDisableStatisticsNotifications(final int index, final boolean enable) {
        WantPerInterfaceCombinedStats request = new WantPerInterfaceCombinedStats();
        request.num = 1;
        request.enableDisable = enable
                ? (byte) 1
                : (byte) 0;
        request.pid = 1;
        request.swIfs = new int[]{index};
        final CompletionStage<WantPerInterfaceCombinedStatsReply> result =
                this.getFutureJVpp().wantPerInterfaceCombinedStats(request);
        try {
            getReply(result.toCompletableFuture());
        } catch (VppBaseCallException | TimeoutException e) {
            String errorMsg = String.format("Unable to %s statistics notifications", enable
                    ? "enable"
                    : "disable");
            LOG.warn(errorMsg, e);
            throw new IllegalStateException(errorMsg, e);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<StatisticsCollection> id,
                                        @Nonnull final StatisticsCollection dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        InterfaceKey key = id.firstKeyOf(Interface.class);
        final int index = ifcNamingContext.getIndex(key.getName(), writeContext.getMappingContext());
        enableDisableStatisticsNotifications(index, false);
        this.ifcStatisticsManager.disableInterface(index);
    }
}
