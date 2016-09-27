/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.fd.honeycomb.lisp.translate.write.RemoteMappingCustomizer.LocatorListType.NEGATIVE;
import static io.fd.honeycomb.lisp.translate.write.RemoteMappingCustomizer.LocatorListType.POSITIVE;

import com.google.common.base.Preconditions;
import io.fd.honeycomb.lisp.context.util.EidMappingContext;
import io.fd.honeycomb.lisp.translate.util.EidTranslator;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.AddressTranslator;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.vpp.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MapReplyAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.MappingId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.RemoteMappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.LocatorList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.NegativeMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.PositiveMapping;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.positive.mapping.Rlocs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.remote.mappings.remote.mapping.locator.list.positive.mapping.rlocs.Locator;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.LispAddDelRemoteMapping;
import org.openvpp.jvpp.core.future.FutureJVppCore;


/**
 * Customizer for {@link RemoteMapping}
 */
public class RemoteMappingCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<RemoteMapping, RemoteMappingKey>, EidTranslator,
        AddressTranslator, JvppReplyConsumer {

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

        //checks whether mapping not already contains such key
        MappingId mappingId = id.firstKeyOf(RemoteMapping.class).getId();
        checkState(!remoteMappingContext.containsEid(mappingId, writeContext.getMappingContext()),
                "Mapping for id %s already defined", mappingId);

        try {
            addDelRemoteMappingAndReply(true, dataAfter,
                    id.firstKeyOf(VniTable.class).getVirtualNetworkIdentifier().intValue());
        } catch (VppBaseCallException | TimeoutException | IOException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

        //after successfull adition adds mapping
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

            request.rlocs = locatorsToBinaryData(rlocs.getLocator());
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

        return ((NegativeMapping) locatorList).getMapReplyAction();
    }

    private static Rlocs extractRemoteLocators(LocatorList locatorList) {
        checkNotNull(locatorList, "Locator List cannot be null");
        Preconditions.checkArgument(POSITIVE.equals(resolveType(locatorList)),
                "RLocs can be extracted only from Positive Mapping");

        return ((PositiveMapping) locatorList).getRlocs();
    }

    //cant be static because of use of default methods from traits
    private byte[] locatorsToBinaryData(List<Locator> locators) throws IOException {
        checkNotNull(locators, "Cannot convert null list");

        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();

        DataOutputStream out = new DataOutputStream(byteArrayOut);


        for (Locator locator : locators) {
            boolean isIpv4;
            byte[] address;

            //first byte says that its v4/v6
            isIpv4 = !isIpv6(locator.getAddress());
            out.writeByte(booleanToByte(isIpv4));

            //then writes priority
            out.write(locator.getPriority());

            //and weight
            out.write(locator.getWeight());

            if (isIpv4) {
                //vpp in this case needs address as 16 byte array,regardless if it is ivp4 or ipv6
                address = Arrays.copyOf(

                        ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(locator.getAddress().getIpv4Address())),
                        16);

                out.write(address);
            } else {
                out.write(
                        ipv6AddressNoZoneToArray(new Ipv6AddressNoZone(locator.getAddress().getIpv6Address())));
            }
        }

        return byteArrayOut.toByteArray();
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
