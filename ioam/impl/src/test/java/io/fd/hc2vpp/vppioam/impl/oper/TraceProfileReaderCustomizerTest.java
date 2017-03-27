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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.ioamtrace.dto.TraceProfileShowConfig;
import io.fd.vpp.jvpp.ioamtrace.dto.TraceProfileShowConfigReply;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.IoamTraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.IoamTraceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfigKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class TraceProfileReaderCustomizerTest extends ListReaderCustomizerTest<TraceConfig, TraceConfigKey, TraceConfigBuilder> {

    @Mock
    protected FutureJVppIoamtrace jVppIoamtrace;

    public TraceProfileReaderCustomizerTest(){
        super(TraceConfig.class, IoamTraceConfigBuilder.class);
    }

    @Override
    protected ReaderCustomizer<TraceConfig, TraceConfigBuilder> initCustomizer() {
        return new TraceProfileReaderCustomizer(jVppIoamtrace);
    }

    @Override
    public void setUp(){
        final TraceProfileShowConfigReply reply = new TraceProfileShowConfigReply();
        reply.appData = 1234;
        reply.traceTsp = ((byte) TraceConfig.TraceTsp.Milliseconds.getIntValue());
        reply.nodeId = 1;
        reply.numElts = 4;
        reply.traceType = 0x1f;
        doReturn(future(reply)).when(jVppIoamtrace).traceProfileShowConfig(any(TraceProfileShowConfig.class));
    }

    @Test
    public void testReadCurrentAttributes() throws ReadFailedException {
        TraceConfigBuilder builder = new TraceConfigBuilder();
        getCustomizer().readCurrentAttributes(InstanceIdentifier.create(IoamTraceConfig.class)
                .child(TraceConfig.class, new TraceConfigKey("Trace config")), builder, ctx);
        assertEquals(1234,builder.getTraceAppData().longValue());
        assertEquals(TraceConfig.TraceTsp.Milliseconds.getIntValue(),builder.getTraceTsp().getIntValue());
        assertEquals(1,builder.getNodeId().longValue());
        assertEquals(4,builder.getTraceNumElt().shortValue());
        assertEquals(0x1f,builder.getTraceType().shortValue());

        verify(jVppIoamtrace).traceProfileShowConfig(any(TraceProfileShowConfig.class));
    }

}
