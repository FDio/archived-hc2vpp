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

package io.fd.honeycomb.translate.v3po.vpp;

import static io.fd.honeycomb.translate.v3po.util.TranslateUtils.booleanToByte;

import com.google.common.base.Preconditions;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.TranslateUtils;
import io.fd.honeycomb.translate.v3po.util.WriteTimeoutException;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.bridge.domain.attributes.arp.termination.table.ArpTerminationTableEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.bridge.domain.attributes.arp.termination.table.ArpTerminationTableEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.bridge.domains.BridgeDomain;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.dto.BdIpMacAddDel;
import org.openvpp.jvpp.core.dto.BdIpMacAddDelReply;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writer Customizer responsible for ARP termination table management.<br> Sends {@code bd_ip_mac_add_del} message to
 * VPP.<br> Equivalent of invoking {@code vppctl set bridge-domain arp term} command.
 */
public class ArpTerminationTableEntryCustomizer extends FutureJVppCustomizer
    implements ListWriterCustomizer<ArpTerminationTableEntry, ArpTerminationTableEntryKey> {

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
        try {
            LOG.debug("Creating ARP termination table entry: {} {}", id, dataAfter);
            bdIpMacAddDel(id, dataAfter, writeContext, true);
            LOG.debug("L2 ARP termination table entry created successfully: {} {}", id, dataAfter);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to create ARP termination table entry: {} {}", id, dataAfter);
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<ArpTerminationTableEntry> id,
                                        @Nonnull final ArpTerminationTableEntry dataBefore,
                                        @Nonnull final ArpTerminationTableEntry dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new UnsupportedOperationException(
            "ARP termination table entry update is not supported. It has to be deleted and then created.");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<ArpTerminationTableEntry> id,
                                        @Nonnull final ArpTerminationTableEntry dataBefore,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        try {
            LOG.debug("Deleting ARP termination table entry entry: {} {}", id, dataBefore);
            bdIpMacAddDel(id, dataBefore, writeContext, false);
            LOG.debug("ARP termination table entry deleted successfully: {} {}", id, dataBefore);
        } catch (VppBaseCallException e) {
            LOG.warn("Failed to delete ARP termination table entry: {} {}", id, dataBefore);
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void bdIpMacAddDel(@Nonnull final InstanceIdentifier<ArpTerminationTableEntry> id,
                               @Nonnull final ArpTerminationTableEntry entry,
                               final WriteContext writeContext, boolean isAdd)
        throws VppBaseCallException, WriteTimeoutException {
        final String bdName = id.firstKeyOf(BridgeDomain.class).getName();
        final int bdId = bdContext.getIndex(bdName, writeContext.getMappingContext());

        final BdIpMacAddDel request = createRequest(entry, bdId, isAdd);
        LOG.debug("Sending l2FibAddDel request: {}", request);
        final CompletionStage<BdIpMacAddDelReply> replyCompletionStage =
            getFutureJVpp().bdIpMacAddDel(request);

        TranslateUtils.getReplyForWrite(replyCompletionStage.toCompletableFuture(), id);
    }

    private BdIpMacAddDel createRequest(final ArpTerminationTableEntry entry, final int bdId, boolean isAdd) {
        final BdIpMacAddDel request = new BdIpMacAddDel();
        request.bdId = bdId;
        request.isAdd = booleanToByte(isAdd);
        request.macAddress = TranslateUtils.parseMac(entry.getPhysAddress().getValue());

        final IpAddress ipAddress = entry.getIpAddress();
        if (ipAddress.getIpv6Address() != null) {
            // FIXME: HONEYCOMB-187 vpp does not support ipv6 in arp-termination table (based on analysis of l2_bd.c)
            throw new UnsupportedOperationException("IPv6 address for ARP termination table is not supported yet");
        }

        request.ipAddress = TranslateUtils.ipv4AddressNoZoneToArray(new Ipv4AddressNoZone(ipAddress.getIpv4Address()));
        return request;
    }
}
