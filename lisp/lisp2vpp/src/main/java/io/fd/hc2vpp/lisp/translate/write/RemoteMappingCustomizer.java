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


import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.lisp.context.util.EidMappingContext;
import io.fd.hc2vpp.lisp.translate.read.trait.MappingProducer;
import io.fd.hc2vpp.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.LispAddDelRemoteMapping;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.RemoteLocator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.LocatorList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.NegativeMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.PositiveMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.dp.subtable.grouping.remote.mappings.remote.mapping.locator.list.positive.mapping.Rlocs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.*;
import static io.fd.hc2vpp.lisp.translate.write.RemoteMappingCustomizer.LocatorListType.NEGATIVE;
import static io.fd.hc2vpp.lisp.translate.write.RemoteMappingCustomizer.LocatorListType.POSITIVE;


/**
 * Customizer for {@link RemoteMapping}
 */
public class RemoteMappingCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<RemoteMapping, RemoteMappingKey>, EidTranslator,
        AddressTranslator, JvppReplyConsumer, MappingProducer {

    private final EidMappingContext remoteMappingContext;

    public RemoteMappingCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                   @Nonnull final EidMappingContext remoteMappingContext) {
        super(futureJvpp);
        this.remoteMappingContext = remoteMappingContext;
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<RemoteMapping> id, RemoteMapping dataAfter,
                                       WriteContext writeContext) throws WriteFailedException {
        checkNotNull(dataAfter, "Mapping is null");
        checkNotNull(dataAfter.getEid(), "Eid is null");
        checkState(id.firstKeyOf(VniTable.class) != null, "Parent vni table not found");
        checkAllowedCombination(id, dataAfter);

        try {
            addDelRemoteMappingAndReply(true, dataAfter,
                    id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue());
        } catch (VppBaseCallException | TimeoutException | IOException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        // after successfull adition adds mapping
        MappingId mappingId = id.firstKeyOf(RemoteMapping.class).getId();
        remoteMappingContext.addEid(mappingId, dataAfter.getEid(), writeContext.getMappingContext());
    }

    @Override
    public void updateCurrentAttributes(InstanceIdentifier<RemoteMapping> id, RemoteMapping dataBefore,
                                        RemoteMapping dataAfter, WriteContext writeContext)
            throws WriteFailedException {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<RemoteMapping> id, RemoteMapping dataBefore,
                                        WriteContext writeContext) throws WriteFailedException {
        checkNotNull(dataBefore, "Mapping is null");
        checkNotNull(dataBefore.getEid(), "Eid is null");

        //checks whether mapping already contains such key
        MappingId mappingId = id.firstKeyOf(RemoteMapping.class).getId();
        checkState(remoteMappingContext.containsEid(mappingId, writeContext.getMappingContext()),
                "Mapping for id %s is not existing,nothing to remove", mappingId);

        try {
            addDelRemoteMappingAndReply(false, dataBefore,
                    id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue());
        } catch (VppBaseCallException | TimeoutException | IOException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }

        //remote mapping after successfull remove of data
        remoteMappingContext.removeEid(mappingId, writeContext.getMappingContext());
    }

    private void addDelRemoteMappingAndReply(boolean add, RemoteMapping data, int vni)
            throws VppBaseCallException, TimeoutException, IOException {

        LispAddDelRemoteMapping request = new LispAddDelRemoteMapping();

        request.isAdd = booleanToByte(add);
        request.vni = vni;
        request.eidType = (byte) getEidType(data.getEid()).getValue();
        request.eid = getEidAsByteArray(data.getEid());

        //this is not length of eid array,but prefix length(bad naming by vpp)
        request.eidLen = getPrefixLength(data.getEid());

        if (LocatorListType.NEGATIVE
                .equals(resolveType(data.getLocatorList()))) {
            request.action = (byte) extractAction(data.getLocatorList()).getIntValue();
        } else {
            Rlocs rlocs = extractRemoteLocators(data.getLocatorList());

            checkArgument(rlocs != null, "No remote locators set for Positive mapping");

            request.rlocs = rlocs.getLocator().stream()
                    .map(locator -> {
                        RemoteLocator remoteLocator = new RemoteLocator();
                        remoteLocator.addr = ipAddressToArray(locator.getAddress());
                        remoteLocator.isIp4 = booleanToByte(!isIpv6(locator.getAddress()));
                        Optional.ofNullable(locator.getPriority())
                                .ifPresent(priority -> remoteLocator.priority = priority.byteValue());
                        Optional.ofNullable(locator.getWeight())
                                .ifPresent(weight -> remoteLocator.weight = weight.byteValue());

                        return remoteLocator;
                    }).toArray(RemoteLocator[]::new);
            request.rlocNum = Integer.valueOf(rlocs.getLocator().size()).byteValue();
        }

        getReply(getFutureJVpp().lispAddDelRemoteMapping(request).toCompletableFuture());
    }

    private static LocatorListType resolveType(LocatorList locatorList) {
        checkNotNull(locatorList, "Locator List cannot be null");

        if (locatorList instanceof PositiveMapping) {
            return POSITIVE;
        } else {
            return NEGATIVE;
        }
    }

    private static MapReplyAction extractAction(LocatorList locatorList) {
        checkNotNull(locatorList, "Locator List cannot be null");
        Preconditions.checkArgument(NEGATIVE.equals(resolveType(locatorList)),
                "Action can be extracted only from Negative Mapping");

        return ((NegativeMapping) locatorList).getMapReply().getMapReplyAction();
    }

    private static Rlocs extractRemoteLocators(LocatorList locatorList) {
        checkNotNull(locatorList, "Locator List cannot be null");
        Preconditions.checkArgument(POSITIVE.equals(resolveType(locatorList)),
                "RLocs can be extracted only from Positive Mapping");

        return ((PositiveMapping) locatorList).getRlocs();
    }

    public enum LocatorListType {

        /**
         * Represents locator list as negative mapping
         */
        NEGATIVE,

        /**
         * Represents locator list as positive mapping
         */
        POSITIVE
    }

}
