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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.ioamexport.dto.IoamExportIp6EnableDisable;
import io.fd.vpp.jvpp.ioamexport.dto.IoamExportIp6EnableDisableReply;
import io.fd.vpp.jvpp.ioamexport.future.FutureJVppIoamexport;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.export.rev170206.IoamExport;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.export.rev170206.IoamExportBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IoamExportWriterCustomizerTest extends WriterCustomizerTest
        implements Ipv4Translator{

    @Mock
    FutureJVppIoamexport jVppIoamexport;

    private IoamExportWriterCustomizer customizer;

    private static final Logger LOG = LoggerFactory.getLogger(IoamExportWriterCustomizerTest.class);

    @Override
    public void setUpTest() throws Exception {
        customizer = new IoamExportWriterCustomizer(jVppIoamexport);
    }

    private static IoamExport generateExportProfile(){
        IoamExportBuilder builder = new IoamExportBuilder();
        builder.setDisable(false);
        builder.setSourceAddress(new Ipv4Address("127.0.0.1"));
        builder.setCollectorAddress(new Ipv4Address("127.0.0.2"));
        return builder.build();
    }

    private void whenExportAddThenSuccess() {
        final IoamExportIp6EnableDisableReply reply = new IoamExportIp6EnableDisableReply();
        reply.context = 1;
        doReturn(future(reply)).when(jVppIoamexport).ioamExportIp6EnableDisable(any(IoamExportIp6EnableDisable.class));
    }

    private void whenExportAddThenFailure() {
        doReturn(failedFuture()).when(jVppIoamexport).ioamExportIp6EnableDisable(any(IoamExportIp6EnableDisable.class));
    }

    private void whenExportDelThenSuccess() {
        final IoamExportIp6EnableDisableReply reply = new IoamExportIp6EnableDisableReply();
        reply.context = 1;
        doReturn(future(reply)).when(jVppIoamexport).ioamExportIp6EnableDisable(any(IoamExportIp6EnableDisable.class));
    }

    private void whenExportDelThenFailure() {
        doReturn(failedFuture()).when(jVppIoamexport).ioamExportIp6EnableDisable(any(IoamExportIp6EnableDisable.class));
    }

    private IoamExportIp6EnableDisable generateIoamExportIp6EnableDisable(boolean disable) {
        IoamExportIp6EnableDisable request = new IoamExportIp6EnableDisable();
        request.isDisable = (byte)(disable?1:0);
        request.srcAddress = ipv4AddressNoZoneToArray("127.0.0.1");
        request.collectorAddress = ipv4AddressNoZoneToArray("127.0.0.2");

        return request;
    }

    @Test
    public void testCreate() throws Exception {
        final IoamExport ioamExport = generateExportProfile();
        final InstanceIdentifier<IoamExport> id = InstanceIdentifier.create(IoamExport.class);

        whenExportAddThenSuccess();

        customizer.writeCurrentAttributes(id, ioamExport, writeContext);

        verify(jVppIoamexport).ioamExportIp6EnableDisable(generateIoamExportIp6EnableDisable(false));
    }

    @Test
    public void testCreateFailed() throws Exception {
        final IoamExport ioamExport = generateExportProfile();
        final InstanceIdentifier<IoamExport> id = InstanceIdentifier.create(IoamExport.class);

        whenExportAddThenFailure();

        try {
            customizer.writeCurrentAttributes(id, ioamExport, writeContext);
        } catch (WriteFailedException e) {
            verify(jVppIoamexport).ioamExportIp6EnableDisable(generateIoamExportIp6EnableDisable(false));

            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testDelete() throws Exception {
        final IoamExport ioamExport = generateExportProfile();
        final InstanceIdentifier<IoamExport> id = InstanceIdentifier.create(IoamExport.class);

        whenExportDelThenSuccess();

        customizer.deleteCurrentAttributes(id, ioamExport, writeContext);

        verify(jVppIoamexport).ioamExportIp6EnableDisable(generateIoamExportIp6EnableDisable(true));
    }

    @Test
    public void testDeleteFailed() throws Exception {
        final IoamExport ioamExport = generateExportProfile();
        final InstanceIdentifier<IoamExport> id = InstanceIdentifier.create(IoamExport.class);

        whenExportDelThenFailure();

        try {
            customizer.deleteCurrentAttributes(id, ioamExport, writeContext);
        } catch (WriteFailedException e) {
            verify(jVppIoamexport).ioamExportIp6EnableDisable(generateIoamExportIp6EnableDisable(true));

            return;
        }
        fail("WriteFailedException.CreateFailedException was expected");
    }

    @Test
    public void testCreateWithMissingDisabledField() throws Exception {
        IoamExportBuilder builder = new IoamExportBuilder();
        builder.setSourceAddress(new Ipv4Address("127.0.0.1"));
        builder.setCollectorAddress(new Ipv4Address("127.0.0.2"));
        final IoamExport ioamExport = builder.build();
        final InstanceIdentifier<IoamExport> id = InstanceIdentifier.create(IoamExport.class);

        whenExportAddThenSuccess();

        customizer.writeCurrentAttributes(id, ioamExport, writeContext);

        verify(jVppIoamexport).ioamExportIp6EnableDisable(generateIoamExportIp6EnableDisable(true));
    }

}
