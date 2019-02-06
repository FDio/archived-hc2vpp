/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.ipsec.write;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpsecSpdAddDel;
import io.fd.vpp.jvpp.core.dto.IpsecSpdEntryAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.IpsecSpdAction;
import io.fd.vpp.jvpp.core.types.IpsecSpdEntry;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecSpdEntriesAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.Spd;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.SpdKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.spd.SpdEntries;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IpsecSpdCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Spd, SpdKey>, JvppReplyConsumer, ByteDataTranslator,
        Ipv6Translator, Ipv4Translator {

    public IpsecSpdCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Spd> id, @Nonnull final Spd dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        IpsecSpdAddDel spdCreate = new IpsecSpdAddDel();
        spdCreate.isAdd = ByteDataTranslator.BYTE_TRUE;
        spdCreate.spdId = dataAfter.getSpdId();
        getReplyForWrite(getFutureJVpp().ipsecSpdAddDel(spdCreate).toCompletableFuture(), id);
        if (dataAfter.getSpdEntries() != null) {
            for (SpdEntries entry : dataAfter.getSpdEntries()) {
                addSpdEntry(id, dataAfter.getSpdId(), entry);
            }
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Spd> id, @Nonnull final Spd dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        IpsecSpdAddDel spdDelete = new IpsecSpdAddDel();
        spdDelete.isAdd = ByteDataTranslator.BYTE_FALSE;
        spdDelete.spdId = dataBefore.getSpdId();
        getReplyForWrite(getFutureJVpp().ipsecSpdAddDel(spdDelete).toCompletableFuture(), id);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Spd> id, @Nonnull final Spd dataBefore,
                                        @Nonnull final Spd dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        if (dataAfter.getSpdEntries() != null) {
            for (SpdEntries entry : dataAfter.getSpdEntries()) {
                addSpdEntry(id, dataAfter.getSpdId(), entry);
            }
        }
    }

    private void addSpdEntry(final InstanceIdentifier<Spd> id, int spdId, final SpdEntries entry)
            throws WriteFailedException {
        IpsecSpdEntryAddDel request = new IpsecSpdEntryAddDel();
        request.entry = new IpsecSpdEntry();
        request.entry.spdId = spdId;
        request.isAdd = ByteDataTranslator.BYTE_TRUE;
        IpsecSpdEntriesAugmentation entryAug = entry.augmentation(IpsecSpdEntriesAugmentation.class);
        if (entryAug == null) {
            return;
        }

        if (entryAug.getDirection() != null) {
            request.entry.isOutbound = (byte) entryAug.getDirection().getIntValue();
        }

        if (entryAug.getPriority() != null) {
            request.entry.priority = entryAug.getPriority();
        }

        if (entryAug.getOperation() != null) {
            final String operation = entryAug.getOperation().getName();
            if (operation.equalsIgnoreCase("bypass")) {
                request.entry.policy = IpsecSpdAction.IPSEC_API_SPD_ACTION_BYPASS;
            } else if (operation.equalsIgnoreCase("discard")) {
                request.entry.policy = IpsecSpdAction.IPSEC_API_SPD_ACTION_DISCARD;
            } else if (operation.equalsIgnoreCase("protect")) {
                request.entry.policy = IpsecSpdAction.IPSEC_API_SPD_ACTION_PROTECT;
            }
        }

        if (entryAug.getLaddrStart() != null) {
            if (entryAug.getLaddrStart().getIpv4Address() != null) {
                request.entry.localAddressStart = ipv4AddressToAddress(entryAug.getLaddrStart().getIpv4Address());
            } else if (entryAug.getLaddrStart().getIpv6Address() != null) {
                request.entry.localAddressStart = ipv6AddressToAddress(entryAug.getLaddrStart().getIpv6Address());
            }
        }
        if (entryAug.getLaddrStop() != null) {
            if (entryAug.getLaddrStop().getIpv4Address() != null) {
                request.entry.localAddressStop = ipv4AddressToAddress(entryAug.getLaddrStop().getIpv4Address());
            } else if (entryAug.getLaddrStop().getIpv6Address() != null) {
                request.entry.localAddressStop = ipv6AddressToAddress(entryAug.getLaddrStop().getIpv6Address());
            }
        }
        if (entryAug.getRaddrStop() != null) {
            if (entryAug.getRaddrStop().getIpv4Address() != null) {
                request.entry.remoteAddressStop = ipv4AddressToAddress(entryAug.getRaddrStop().getIpv4Address());
            } else if (entryAug.getRaddrStop().getIpv6Address() != null) {
                request.entry.remoteAddressStop = ipv6AddressToAddress(entryAug.getRaddrStop().getIpv6Address());
            }
        }

        if (entryAug.getRaddrStart() != null) {
            if (entryAug.getRaddrStart().getIpv4Address() != null) {
                request.entry.remoteAddressStart = ipv4AddressToAddress(entryAug.getRaddrStart().getIpv4Address());
            } else if (entryAug.getRaddrStart().getIpv6Address() != null) {
                request.entry.remoteAddressStart = ipv6AddressToAddress(entryAug.getRaddrStart().getIpv6Address());
            }
        }

        //TODO HC2VPP-403: missing local and remote port definitions
        getReplyForWrite(getFutureJVpp().ipsecSpdEntryAddDel(request).toCompletableFuture(), id);
    }
}
