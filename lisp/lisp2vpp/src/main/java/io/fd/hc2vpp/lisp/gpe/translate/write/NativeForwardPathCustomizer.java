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

package io.fd.hc2vpp.lisp.gpe.translate.write;

import static io.fd.hc2vpp.lisp.gpe.translate.write.NativeForwardPathsTableCustomizer.tableId;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.GpeAddDelNativeFwdRpath;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.gpe.rev170801._native.forward.paths.tables._native.forward.paths.table.NativeForwardPathKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NativeForwardPathCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<NativeForwardPath, NativeForwardPathKey>, AddressTranslator, JvppReplyConsumer {

    private final NamingContext interfaceContext;

    public NativeForwardPathCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                       @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<NativeForwardPath> id,
                                       @Nonnull final NativeForwardPath dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        createNativePath(id, dataAfter, writeContext);
    }


    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<NativeForwardPath> id,
                                        @Nonnull final NativeForwardPath dataBefore,
                                        @Nonnull final NativeForwardPath dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        deleteNativePath(id, dataBefore, writeContext);
        createNativePath(id, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<NativeForwardPath> id,
                                        @Nonnull final NativeForwardPath dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        deleteNativePath(id, dataBefore, writeContext);
    }

    private GpeAddDelNativeFwdRpath getRequest(final boolean isAdd,
                                               final int tableId,
                                               final NativeForwardPath data,
                                               final MappingContext mappingContext) {
        GpeAddDelNativeFwdRpath request = new GpeAddDelNativeFwdRpath();

        final IpAddress nextHopAddress = data.getNextHopAddress();

        request.tableId = tableId;
        request.isAdd = booleanToByte(isAdd);
        request.isIp4 = booleanToByte(!isIpv6(nextHopAddress));
        request.nhAddr = ipAddressToArray(nextHopAddress);
        request.nhSwIfIndex = Optional.ofNullable(data.getNextHopInterface())
                .map(String::trim)
                .map(ifaceName -> interfaceContext.getIndex(ifaceName, mappingContext))
                .orElse(~0);

        return request;
    }

    private void createNativePath(final InstanceIdentifier<NativeForwardPath> id,
                                  final NativeForwardPath data,
                                  final WriteContext ctx) throws WriteFailedException {
        getReplyForCreate(getFutureJVpp()
                .gpeAddDelNativeFwdRpath(getRequest(true, tableId(id), data, ctx.getMappingContext()))
                .toCompletableFuture(), id, data);
    }

    private void deleteNativePath(final InstanceIdentifier<NativeForwardPath> id,
                                  final NativeForwardPath data,
                                  final WriteContext ctx) throws WriteFailedException {
        getReplyForDelete(getFutureJVpp()
                .gpeAddDelNativeFwdRpath(getRequest(false, tableId(id), data, ctx.getMappingContext()))
                .toCompletableFuture(), id);
    }
}
