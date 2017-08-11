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

package io.fd.hc2vpp.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.hc2vpp.lisp.translate.util.CheckedLispCustomizer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.OneAddDelMapResolver;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170808.map.resolvers.grouping.map.resolvers.MapResolverKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Handles updates of {@link MapResolver} list
 */
public class MapResolverCustomizer extends CheckedLispCustomizer
        implements ListWriterCustomizer<MapResolver, MapResolverKey>, AddressTranslator,
        JvppReplyConsumer {

    public MapResolverCustomizer(@Nonnull final FutureJVppCore vppApi,
                                 @Nonnull final LispStateCheckService lispStateCheckService) {
        super(vppApi, lispStateCheckService);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<MapResolver> id,
                                       @Nonnull final MapResolver dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        lispStateCheckService.checkLispEnabledAfter(writeContext);
        checkNotNull(dataAfter, "Data is null");
        checkNotNull(dataAfter.getIpAddress(), "Address is null");

        try {
            addDelMapResolverAndReply(true, dataAfter);
        } catch (VppBaseCallException | TimeoutException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<MapResolver> id,
                                        @Nonnull final MapResolver dataBefore, @Nonnull final MapResolver dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<MapResolver> id,
                                        @Nonnull final MapResolver dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        lispStateCheckService.checkLispEnabledBefore(writeContext);
        checkNotNull(dataBefore, "Data is null");
        checkNotNull(dataBefore.getIpAddress(), "Address is null");

        try {
            addDelMapResolverAndReply(false, dataBefore);
        } catch (VppBaseCallException | TimeoutException e) {
            throw new WriteFailedException.CreateFailedException(id, dataBefore, e);
        }
    }

    private void addDelMapResolverAndReply(boolean add, MapResolver data) throws VppBaseCallException,
            TimeoutException {

        OneAddDelMapResolver request = new OneAddDelMapResolver();
        request.isAdd = booleanToByte(add);


        boolean ipv6 = isIpv6(data.getIpAddress());

        request.isIpv6 = booleanToByte(ipv6);
        request.ipAddress = ipAddressToArray(ipv6, data.getIpAddress());

        getReply(getFutureJVpp().oneAddDelMapResolver(request).toCompletableFuture());
    }
}
