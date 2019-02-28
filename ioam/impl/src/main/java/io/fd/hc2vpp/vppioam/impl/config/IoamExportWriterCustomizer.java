/*
 * Copyright (c) 2016 Cisco and its affiliates.
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

import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.vppioam.impl.util.FutureJVppIoamexportCustomizer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.ioamexport.dto.IoamExportIp6EnableDisable;
import io.fd.jvpp.ioamexport.dto.IoamExportIp6EnableDisableReply;
import io.fd.jvpp.ioamexport.future.FutureJVppIoamexport;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.ioam.sb.export.rev170206.IoamExport;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoamExportWriterCustomizer extends FutureJVppIoamexportCustomizer
        implements WriterCustomizer<IoamExport>, JvppReplyConsumer, Ipv4Translator {

    private static final Logger LOG = LoggerFactory.getLogger(IoamExportWriterCustomizer.class);

    public IoamExportWriterCustomizer(FutureJVppIoamexport jVppIoamExport){
        super(jVppIoamExport);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<IoamExport> instanceIdentifier,
                                       @Nonnull final IoamExport ioamExport, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        addExportProfile(ioamExport,instanceIdentifier);
        LOG.info("Export profile {} created, id: {}", ioamExport, instanceIdentifier);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<IoamExport> instanceIdentifier,
                                        @Nonnull final IoamExport dataBefore, @Nonnull final IoamExport dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        addExportProfile(dataAfter,instanceIdentifier);
        LOG.info("Export profile {} updated , id: {}", dataAfter, instanceIdentifier);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<IoamExport> instanceIdentifier,
                                        @Nonnull final IoamExport ioamExport, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        deleteExportProfile(ioamExport,instanceIdentifier);
        LOG.info("Export profile {} deleted, id: {}", ioamExport, instanceIdentifier);
    }

    private IoamExportIp6EnableDisableReply addExportProfile(IoamExport ioamExport, InstanceIdentifier<IoamExport> id)
            throws WriteFailedException {
        IoamExportIp6EnableDisable request = new IoamExportIp6EnableDisable();
        if (ioamExport.isDisable() == null) {
            request.isDisable = 1;
        } else {
            request.isDisable = (byte)(ioamExport.isDisable() ? 1 : 0);
        }
        request.srcAddress = ipv4AddressNoZoneToArray(ioamExport.getSourceAddress().getValue());
        request.collectorAddress = ipv4AddressNoZoneToArray(ioamExport.getCollectorAddress().getValue());
        return getReplyForCreate(getFutureJVppIoamexport()
                .ioamExportIp6EnableDisable(request)
                .toCompletableFuture(),id,ioamExport);

    }

    private IoamExportIp6EnableDisableReply deleteExportProfile(IoamExport ioamExport,
                                                                InstanceIdentifier<IoamExport> id)
            throws WriteFailedException {
        IoamExportIp6EnableDisable request = new IoamExportIp6EnableDisable();
        request.isDisable = 1;     //disable when deleted
        request.srcAddress = ipv4AddressNoZoneToArray(ioamExport.getSourceAddress().getValue());
        request.collectorAddress = ipv4AddressNoZoneToArray(ioamExport.getCollectorAddress().getValue());

        return getReplyForDelete(getFutureJVppIoamexport()
                .ioamExportIp6EnableDisable(request)
                .toCompletableFuture(),id);
    }

}
