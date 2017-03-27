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

package io.fd.hc2vpp.vppioam.impl.config;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.ioamtrace.dto.TraceProfileAdd;
import io.fd.vpp.jvpp.ioamtrace.dto.TraceProfileAddReply;
import io.fd.vpp.jvpp.ioamtrace.dto.TraceProfileDel;
import io.fd.vpp.jvpp.ioamtrace.dto.TraceProfileDelReply;
import io.fd.vpp.jvpp.ioamtrace.future.FutureJVppIoamtrace;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.IoamTraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfig;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfig.TraceOp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfig.TraceTsp;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.trace.rev170327.ioam.trace.config.TraceConfigKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IoamTraceWriterCustomizerTest extends WriterCustomizerTest {

    private static final String TRACE_NAME = "trace_test";

    @Mock
    protected FutureJVppIoamtrace jvppIoamtrace;

    private IoamTraceWriterCustomizer customizer;

    private static TraceConfig generateTraceConfig(final String name) {
        final TraceConfigBuilder builder = new TraceConfigBuilder();
        builder.setTraceConfigName(name);
        builder.setKey(new TraceConfigKey(name));
        builder.setAclName(name);
        builder.setTraceType(Short.valueOf("31"));
        builder.setTraceNumElt(Short.valueOf("4"));
        builder.setTraceTsp(TraceTsp.Milliseconds);
        builder.setTraceOp(TraceOp.Add);
        builder.setTraceAppData(Long.valueOf("123"));
        builder.setNodeId(Long.valueOf("1"));

        return builder.build();
    }

    private static InstanceIdentifier<TraceConfig> getTraceConfigId(final String name) {
        return InstanceIdentifier.create(IoamTraceConfig.class)
                .child(TraceConfig.class, new TraceConfigKey(name));
    }

    private static TraceProfileAdd generateTraceProfileAdd() {
        final TraceProfileAdd request = new TraceProfileAdd();
        request.traceType = 0x1f;
        request.numElts = 4;
        request.nodeId = 1;
        request.traceTsp = 1;
        request.appData = 123;

        return request;
    }

    private static TraceProfileDel generateTraceProfileDel() {
        final TraceProfileDel request = new TraceProfileDel();

        return request;
    }

    @Override
    public void setUpTest() throws Exception {
        customizer = new IoamTraceWriterCustomizer(jvppIoamtrace);
    }

    private void whenTraceAddThenSuccess() {
        final TraceProfileAddReply reply = new TraceProfileAddReply();
        reply.context = 1;
        doReturn(future(reply)).when(jvppIoamtrace).traceProfileAdd(any(TraceProfileAdd.class));
    }

    private void whenTraceAddThenFailure() {
        doReturn(failedFuture()).when(jvppIoamtrace).traceProfileAdd(any(TraceProfileAdd.class));
    }

    private void whenTraceDelThenSuccess() {
        final TraceProfileDelReply reply = new TraceProfileDelReply();
        reply.context = 1;
        doReturn(future(reply)).when(jvppIoamtrace).traceProfileDel(any(TraceProfileDel.class));
    }

    private void whenTraceDelThenFailure() {
        doReturn(failedFuture()).when(jvppIoamtrace).traceProfileDel(any(TraceProfileDel.class));
    }

    @Test
    public void testCreate() throws Exception {
        final TraceConfig traceConfig = generateTraceConfig(TRACE_NAME);
        final InstanceIdentifier<TraceConfig> id = getTraceConfigId(TRACE_NAME);

        whenTraceAddThenSuccess();

        customizer.writeCurrentAttributes(id, traceConfig, writeContext);

        verify(jvppIoamtrace).traceProfileAdd(generateTraceProfileAdd());
    }

    @Test
    public void testCreateFailed() throws Exception {
        final TraceConfig traceConfig = generateTraceConfig(TRACE_NAME);
        final InstanceIdentifier<TraceConfig> id = getTraceConfigId(TRACE_NAME);

        whenTraceAddThenFailure();

        try {
            customizer.writeCurrentAttributes(id, traceConfig, writeContext);
        } catch (WriteFailedException e) {
            //assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(jvppIoamtrace).traceProfileAdd(generateTraceProfileAdd());

            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testDelete() throws Exception {

        final TraceConfig traceConfig = generateTraceConfig(TRACE_NAME);
        final InstanceIdentifier<TraceConfig> id = getTraceConfigId(TRACE_NAME);

        whenTraceDelThenSuccess();

        customizer.deleteCurrentAttributes(id, traceConfig, writeContext);

        verify(jvppIoamtrace).traceProfileDel(generateTraceProfileDel());
    }

    @Test
    public void testDeleteFailed() throws Exception {

        final TraceConfig traceConfig = generateTraceConfig(TRACE_NAME);
        final InstanceIdentifier<TraceConfig> id = getTraceConfigId(TRACE_NAME);

        whenTraceDelThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, traceConfig, writeContext);
        } catch (WriteFailedException e) {
            //assertTrue(e.getCause() instanceof VppBaseCallException);
            verify(jvppIoamtrace).traceProfileDel(generateTraceProfileDel());

            return;
        }
        fail("WriteFailedException.DeleteFailedException was expected");

        customizer.deleteCurrentAttributes(id, traceConfig, writeContext);
    }
}
