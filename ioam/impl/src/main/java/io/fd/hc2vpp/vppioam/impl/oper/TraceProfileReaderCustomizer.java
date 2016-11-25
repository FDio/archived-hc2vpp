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
package io.fd.hc2vpp.vppioam.impl.oper;

import com.google.common.collect.Lists;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.vppioam.impl.util.FutureJVppIoamtraceCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.Initialized;
import io.fd.honeycomb.translate.spi.read.InitializingListReaderCustomizer;
import io.fd.vpp.jvpp.ioamtrace.dto.TraceProfileShowConfig;
import io.fd.vpp.jvpp.ioamtrace.dto.TraceProfileShowConfigReply;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.IoamTraceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.ioam.trace.config.TraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.ioam.trace.config.TraceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.ioam.trace.config.TraceConfigKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceProfileReaderCustomizer extends FutureJVppIoamtraceCustomizer
        implements InitializingListReaderCustomizer<TraceConfig, TraceConfigKey, TraceConfigBuilder>, JvppReplyConsumer{

    private static final Logger LOG = LoggerFactory.getLogger(TraceProfileReaderCustomizer.class);

    public TraceProfileReaderCustomizer(@Nonnull FutureJVppIoamtrace futureJVppIoamtrace) {

        super(futureJVppIoamtrace);
    }

    /**
     * Creates new builder that will be used to build read value.
     *
     * @param id
     */
    @Nonnull
    @Override
    public TraceConfigBuilder getBuilder(@Nonnull InstanceIdentifier<TraceConfig> id) {
        return new TraceConfigBuilder();
    }

    /**
     * Transform Operational data into Config data.
     *
     * @param id        InstanceIdentifier of operational data being initialized
     * @param readValue Operational data being initialized(converted into config)
     * @param ctx       Standard read context to assist during initialization e.g. caching data between customizers
     * @return Initialized, config data and its identifier
     */
    @Nonnull
    @Override
    public Initialized<? extends DataObject> init(@Nonnull InstanceIdentifier<TraceConfig> id, @Nonnull TraceConfig readValue, @Nonnull ReadContext ctx) {
        return Initialized.create(id,new TraceConfigBuilder(readValue).build());
    }

    /**
     * Adds current data (identified by id) to the provided builder.
     *
     * @param id      id of current data object
     * @param builder builder for creating read value
     * @param ctx     context for current read
     * @throws ReadFailedException if read was unsuccessful
     */
    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<TraceConfig> id, @Nonnull TraceConfigBuilder builder,
                                      @Nonnull ReadContext ctx) throws ReadFailedException {

        // Only one trace config is configured on the VPP side as of now.
        // name of trace config, which is key for the list, is not used in the VPP implementation.

        LOG.debug("reading attribute for trace config {}",id);
        final TraceProfileShowConfig request = new TraceProfileShowConfig();
        TraceProfileShowConfigReply reply = getReplyForRead(getFutureJVppIoamtrace().traceProfileShowConfig(request)
                .toCompletableFuture(),id);
        if(reply == null) {
            LOG.debug("{} returned null as reply from vpp",id);
            return;
        }

        if(reply.traceType == 0){
            LOG.debug("{} no configured trace config found",id);
            return;
        }
        builder.setNodeId((long) reply.nodeId);
        builder.setTraceAppData((long) reply.appData);
        builder.setTraceNumElt((short) reply.numElts);
        builder.setTraceTsp(TraceConfig.TraceTsp.forValue(reply.traceTsp));
        builder.setTraceType((short) reply.traceType);

        LOG.debug("Item {} successfully read: {}",id,builder.build());
    }

    /**
     * Merge read data into provided parent builder.
     *
     * @param parentBuilder
     * @param readValue
     */
    @Override
    public void merge(@Nonnull Builder<? extends DataObject> parentBuilder, @Nonnull TraceConfig readValue) {
        IoamTraceConfigBuilder builder = (IoamTraceConfigBuilder) parentBuilder;
        List<TraceConfig> traceConfigs = builder.getTraceConfig();
        traceConfigs.add(readValue);
        builder.setTraceConfig(traceConfigs);
    }

    @Nonnull
    @Override
    public List<TraceConfigKey> getAllIds(@Nonnull final InstanceIdentifier<TraceConfig> instanceIdentifier,
                                          @Nonnull final ReadContext readContext) throws ReadFailedException {

        // Only one trace config is configured on the VPP side as of now.
        // name of trace config, which is key for the list, is not used in the VPP implementation.

        return Lists.newArrayList(new TraceConfigKey("trace config"));
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<TraceConfig> list) {
        ((IoamTraceConfigBuilder) builder).setTraceConfig(list);
    }
}
