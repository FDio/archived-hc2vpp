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

import io.fd.honeycomb.translate.impl.read.GenericInitListReader;
import io.fd.honeycomb.translate.read.ReaderFactory;
import io.fd.honeycomb.translate.read.registry.ModifiableReaderRegistryBuilder;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.IoamTraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.IoamTraceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev160512.ioam.trace.config.TraceConfig;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VppIoamReaderFactory implements ReaderFactory {

    FutureJVppIoamtrace jVppIoamtrace;

    @Inject
    VppIoamReaderFactory(FutureJVppIoamtrace jVppIoamtrace){

        this.jVppIoamtrace = jVppIoamtrace;
    }

    /**
     * Initialize 1 or more readers and add them to provided registry.
     *
     * @param registry
     */
    @Override
    public void init(@Nonnull ModifiableReaderRegistryBuilder registry) {

        //IoamTraceConfig (Structural)
        final InstanceIdentifier<IoamTraceConfig> ioamTraceConfigId = InstanceIdentifier.create(IoamTraceConfig.class);
        registry.addStructuralReader(ioamTraceConfigId, IoamTraceConfigBuilder.class);

        //TraceConfig
        final InstanceIdentifier<TraceConfig> traceConfigId = ioamTraceConfigId.child(TraceConfig.class);
        registry.add(new GenericInitListReader<>(traceConfigId,
                new TraceProfileReaderCustomizer(jVppIoamtrace)));
    }
}
