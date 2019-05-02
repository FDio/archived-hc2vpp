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

package io.fd.hc2vpp.v3po.l2;

import com.google.common.base.Preconditions;
import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.BdIpMacAddDel;
import io.fd.jvpp.core.dto.BdIpMacAddDelReply;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.Address;
import io.fd.jvpp.core.types.AddressFamily;
import io.fd.jvpp.core.types.AddressUnion;
import io.fd.jvpp.core.types.Ip4Address;
import io.fd.jvpp.core.types.MacAddress;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.bridge.domain.attributes.arp.termination.table.ArpTerminationTableEntry;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.bridge.domain.attributes.arp.termination.table.ArpTerminationTableEntryKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressNoZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for ARP termination table management.<br> Sends {@code bd_ip_mac_add_del} message to
 * VPP.<br> Equivalent of invoking {@code vppctl set bridge-domain arp term} command.
 */
public class ArpTerminationTableEntryCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<ArpTerminationTableEntry, ArpTerminationTableEntryKey>, ByteDataTranslator,
        AddressTranslator, JvppReplyConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(ArpTerminationTableEntryCustomizer.class);

    private final NamingContext bdContext;

    public ArpTerminationTableEntryCustomizer(@Nonnull final FutureJVppCore futureJvpp,
                                              @Nonnull final NamingContext bdContext) {
        super(futureJvpp);
        this.bdContext = Preconditions.checkNotNull(bdContext, "bdContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<ArpTerminationTableEntry> id,
                                       @Nonnull final ArpTerminationTableEntry dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Creating ARP termination table entry: {} {}", id, dataAfter);
        bdIpMacAddDel(id, dataAfter, writeContext, true);
        LOG.debug("L2 ARP termination table entry created successfully: {} {}", id, dataAfter);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ArpTerminationTableEntry> id,
                                        @Nonnull final ArpTerminationTableEntry dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Deleting ARP termination table entry entry: {} {}", id, dataBefore);
        bdIpMacAddDel(id, dataBefore, writeContext, false);
        LOG.debug("ARP termination table entry deleted successfully: {} {}", id, dataBefore);
    }

    private void bdIpMacAddDel(@Nonnull final InstanceIdentifier<ArpTerminationTableEntry> id,
                               @Nonnull final ArpTerminationTableEntry entry,
                               final WriteContext writeContext, boolean isAdd) throws WriteFailedException {
        final String bdName = id.firstKeyOf(BridgeDomain.class).getName();
        final int bdId = bdContext.getIndex(bdName, writeContext.getMappingContext());

        final BdIpMacAddDel request = createRequest(entry, bdId, isAdd);
        LOG.debug("Sending l2FibAddDel request: {}", request);
        final CompletionStage<BdIpMacAddDelReply> replyCompletionStage =
                getFutureJVpp().bdIpMacAddDel(request);

        getReplyForWrite(replyCompletionStage.toCompletableFuture(), id);
    }

    private BdIpMacAddDel createRequest(final ArpTerminationTableEntry entry, final int bdId, boolean isAdd) {
        final BdIpMacAddDel request = new BdIpMacAddDel();
        request.bdId = bdId;
        request.isAdd = booleanToByte(isAdd);
        MacAddress macAddress = new MacAddress();
        macAddress.macaddress = parseMac(entry.getPhysAddress().getValue());
        request.mac = macAddress;
        final IpAddressNoZone ipAddress = entry.getIpAddress();
        Ip4Address ip4Address = new Ip4Address();
        ip4Address.ip4Address = ipAddressToArray(ipAddress);
        Address address = new Address();
        address.af = AddressFamily.ADDRESS_IP4;
        address.un = new AddressUnion(ip4Address);
        request.ip = address;

        return request;
    }
}
