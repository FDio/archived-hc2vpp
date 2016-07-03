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

package io.fd.honeycomb.v3po.translate.v3po.interfaces.ip;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.v3po.translate.v3po.util.WriteTimeoutException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.Neighbor;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces._interface.ipv4.NeighborKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.dto.IpNeighborAddDel;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Customizer for writing {@link Neighbor} for {@link Ipv4}
 */
public class Ipv4NeighbourCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Neighbor, NeighborKey> {


    private static final Logger LOG = LoggerFactory.getLogger(Ipv4NeighbourCustomizer.class);
    final NamingContext interfaceContext;

    public Ipv4NeighbourCustomizer(final FutureJVpp futureJvpp, final NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull InstanceIdentifier<Neighbor> id, @Nonnull Neighbor dataAfter,
                                       @Nonnull WriteContext writeContext)
            throws WriteFailedException {

        checkNotNull(dataAfter, "Cannot write null neighbour");
        checkArgument(id.firstKeyOf(Interface.class) != null, "No parent interface key found");

        LOG.debug("Processing request for Neigbour write");
        String interfaceName = id.firstKeyOf(Interface.class).getName();
        MappingContext mappingContext = writeContext.getMappingContext();

        checkState(interfaceContext.containsIndex(interfaceName, mappingContext),
                "Mapping does not contains mapping for provider interface name ".concat(interfaceName));

        LOG.debug("Parent interface index found");
        try {
            addDelNeighbourAndReply(id, true,
                    interfaceContext.getIndex(interfaceName, mappingContext), dataAfter);
            LOG.info("Neighbour successfully written");
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull InstanceIdentifier<Neighbor> id, @Nonnull Neighbor dataBefore,
                                        @Nonnull Neighbor dataAfter,
                                        @Nonnull WriteContext writeContext) throws WriteFailedException {
        new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull InstanceIdentifier<Neighbor> id, @Nonnull Neighbor dataBefore,
                                        @Nonnull WriteContext writeContext)
            throws WriteFailedException {

        checkNotNull(dataBefore, "Cannot delete null neighbour");
        checkArgument(id.firstKeyOf(Interface.class) != null, "No parent interface key found");

        LOG.debug("Processing request for Neigbour delete");
        String interfaceName = id.firstKeyOf(Interface.class).getName();
        MappingContext mappingContext = writeContext.getMappingContext();

        checkState(interfaceContext.containsIndex(interfaceName, mappingContext),
                "Mapping does not contains mapping for provider interface name %s", interfaceName);

        LOG.debug("Parent interface[{}] index found", interfaceName);
        try {
            addDelNeighbourAndReply(id, false,
                    interfaceContext.getIndex(interfaceName, mappingContext), dataBefore);
            LOG.info("Neighbour {} successfully deleted", id);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    @Override
    public Optional<List<Neighbor>> extract(@Nonnull InstanceIdentifier<Neighbor> currentId,
                                            @Nonnull DataObject parentData) {
        return Optional.fromNullable((((Ipv4) parentData).getNeighbor()));
    }

    private void addDelNeighbourAndReply(InstanceIdentifier<Neighbor> id, boolean add, int parentInterfaceIndex,
                                         Neighbor data)
            throws VppBaseCallException, WriteTimeoutException {

        IpNeighborAddDel request = new IpNeighborAddDel();

        request.isAdd = TranslateUtils.booleanToByte(add);
        request.isIpv6 = 0;
        request.isStatic = 1;
        request.dstAddress = TranslateUtils.ipv4AddressNoZoneToArray(data.getIp());
        request.macAddress = TranslateUtils.parseMac(data.getLinkLayerAddress().getValue());
        request.swIfIndex = parentInterfaceIndex;

        //TODO if it is necessary for future use ,make adjustments to be able to set vrfid
        //request.vrfId
        TranslateUtils.getReplyForWrite(getFutureJVpp().ipNeighborAddDel(request).toCompletableFuture(), id);
    }
}