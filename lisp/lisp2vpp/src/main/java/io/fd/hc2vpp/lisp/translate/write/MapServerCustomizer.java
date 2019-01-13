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

package io.fd.hc2vpp.lisp.translate.write;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.OneAddDelMapServer;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServer;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.map.servers.grouping.map.servers.MapServerKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class MapServerCustomizer extends CheckedLispCustomizer
        implements ListWriterCustomizer<MapServer, MapServerKey>, AddressTranslator,
        JvppReplyConsumer {

    public MapServerCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                               @Nonnull final LispStateCheckService lispStateCheckService) {
        super(futureJVppCore, lispStateCheckService);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<MapServer> instanceIdentifier,
                                       @Nonnull MapServer mapServer,
                                       @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        addDelMapServer(true, instanceIdentifier, mapServer);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<MapServer> instanceIdentifier, @Nonnull MapServer mapServer, @Nonnull WriteContext writeContext) throws WriteFailedException {
        lispStateCheckService.checkLispEnabledBefore(writeContext);
        addDelMapServer(false, instanceIdentifier, mapServer);
    }

    private void addDelMapServer(final boolean add,
                                 @Nonnull final InstanceIdentifier<MapServer> id,
                                 @Nonnull final MapServer data) throws WriteFailedException {
        OneAddDelMapServer request = new OneAddDelMapServer();

        final IpAddress ipAddress = data.getIpAddress();

        request.isAdd = booleanToByte(add);
        request.isIpv6 = booleanToByte(isIpv6(ipAddress));
        request.ipAddress = ipAddressToArray(ipAddress);

        getReplyForWrite(getFutureJVpp().oneAddDelMapServer(request).toCompletableFuture(), id);
    }
}
