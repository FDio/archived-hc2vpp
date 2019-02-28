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

package io.fd.hc2vpp.l3.utils.ip.read;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.jvpp.core.dto.IpNeighborDetails;
import io.fd.jvpp.core.dto.IpNeighborDetailsReplyDump;
import io.fd.jvpp.core.dto.IpNeighborDump;
import io.fd.jvpp.core.future.FutureJVppCore;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.vlan.rev180319.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Provides logic for reading ip neighbours
 */
public abstract class IpNeighbourReader extends IpReader {

    private final DumpCacheManager<IpNeighborDetailsReplyDump, IfaceDumpFilter> dumpCacheManager;

    protected IpNeighbourReader(@Nonnull final NamingContext interfaceContext, boolean isIpv6,
                                @Nonnull final DumpCacheManager<IpNeighborDetailsReplyDump, IfaceDumpFilter> dumpCacheManager) {
        super(interfaceContext, isIpv6);
        this.dumpCacheManager = dumpCacheManager;
    }

    @Nonnull
    protected Optional<IpNeighborDetailsReplyDump> interfaceNeighboursDump(@Nonnull final InstanceIdentifier<?> id,
                                                                           @Nonnull final ReadContext context) throws ReadFailedException {
        return dumpCacheManager.getDump(id, context.getModificationCache(), new IfaceDumpFilter(getInterfaceContext()
                .getIndex(id.firstKeyOf(Interface.class).getName(), context.getMappingContext()), isIpv6()));
    }

    @Nonnull
    protected Optional<IpNeighborDetailsReplyDump> subInterfaceNeighboursDump(@Nonnull final InstanceIdentifier<?> id,
                                                                              @Nonnull final ReadContext context) throws ReadFailedException {
        final String subInterfaceName = SubInterfaceUtils.getSubInterfaceName(id.firstKeyOf(Interface.class).getName(),
                id.firstKeyOf(SubInterface.class).getIdentifier().intValue());
        return dumpCacheManager.getDump(id, context.getModificationCache(), new IfaceDumpFilter(getInterfaceContext()
                .getIndex(subInterfaceName, context.getMappingContext()), isIpv6()));
    }

    @Nonnull
    protected static EntityDumpExecutor<IpNeighborDetailsReplyDump, IfaceDumpFilter> createNeighbourDumpExecutor(
            @Nonnull final FutureJVppCore vppApi) {
        return (identifier, params) -> {
            checkNotNull(params, "Address dump params cannot be null");

            final IpNeighborDump dumpRequest = new IpNeighborDump();
            dumpRequest.isIpv6 = ByteDataTranslator.INSTANCE.booleanToByte(params.isIpv6());
            dumpRequest.swIfIndex = params.getInterfaceIndex();

            return JvppReplyConsumer.INSTANCE.getReplyForRead(vppApi.ipNeighborDump(dumpRequest).toCompletableFuture(), identifier);
        };
    }

    @Nonnull
    protected <T extends Identifier> List<T> getNeighborKeys(
            final Optional<IpNeighborDetailsReplyDump> neighbourDumpOpt,
            final Function<IpNeighborDetails, T> detailToKey)
            throws ReadFailedException {
        if (neighbourDumpOpt.isPresent()) {
            final IpNeighborDetailsReplyDump neighbourDump = neighbourDumpOpt.get();

            return neighbourDump.ipNeighborDetails.stream()
                    .map(detailToKey)
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
