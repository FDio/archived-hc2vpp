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
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.IpsecSadAddDelEntry;
import io.fd.vpp.jvpp.core.dto.IpsecSadAddDelEntryReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecSadEntriesAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ip.address.grouping.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ip.address.grouping.ip.address.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ip.address.grouping.ip.address.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.Ah;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.AuthenticationAlgorithm;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.Esp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.authentication.authentication.algorithm.HmacMd596;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.authentication.authentication.algorithm.HmacSha196;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.EncryptionAlgorithm;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.Aes128Cbc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.Aes192Cbc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.Aes256Cbc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.DesCbc;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sad.SadEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sad.SadEntriesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpsecSadEntryCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<SadEntries, SadEntriesKey>,
        JvppReplyConsumer, ByteDataTranslator, Ipv6Translator, Ipv4Translator {

    private static final Logger LOG = LoggerFactory.getLogger(IpsecSadEntryCustomizer.class);
    private MultiNamingContext sadEntryMapping;

    IpsecSadEntryCustomizer(final FutureJVppCore vppApi, final MultiNamingContext sadEntryMapping) {
        super(vppApi);
        this.sadEntryMapping = sadEntryMapping;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<SadEntries> id,
                                       @Nonnull final SadEntries dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        addDelEntry(id, dataAfter, writeContext, true);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<SadEntries> id,
                                        @Nonnull final SadEntries dataBefore,
                                        @Nonnull final SadEntries dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        writeCurrentAttributes(id, dataAfter, writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<SadEntries> id,
                                        @Nonnull final SadEntries dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        addDelEntry(id, dataBefore, writeContext, false);
    }

    private void addDelEntry(final InstanceIdentifier<SadEntries> id,
                             final SadEntries dataAfter,
                             final WriteContext writeContext, boolean adding) throws WriteFailedException {
        final IpsecSadAddDelEntry entry = new IpsecSadAddDelEntry();
        IpsecSadEntriesAugmentation augment = dataAfter.augmentation(IpsecSadEntriesAugmentation.class);
        if (augment != null && augment.getSaId() != null) {
            entry.sadId = augment.getSaId();
        }
        if (dataAfter.getSpi() != null) {
            entry.spi = dataAfter.getSpi().intValue();
        }
        if (dataAfter.getAntiReplayWindow() != null) {
            entry.useAntiReplay = dataAfter.getAntiReplayWindow() > 0
                    ? BYTE_TRUE
                    : BYTE_FALSE;
        }

        if (dataAfter.getSaMode() != null) {
            entry.isTunnel = Integer.valueOf(dataAfter.getSaMode().getIntValue()).byteValue();
        }
        entry.isAdd = adding
                ? ByteDataTranslator.BYTE_TRUE
                : ByteDataTranslator.BYTE_FALSE;
        if (dataAfter.getEsp() != null) {
            entry.protocol = 1;
            fillEspAuthentication(entry, dataAfter.getEsp());
            fillEspEncryption(entry, dataAfter.getEsp());

        } else if (dataAfter.getAh() != null) {
            entry.protocol = 0;
            fillAhAuthentication(entry, dataAfter.getAh());
        }

        fillAddresses(entry, dataAfter);

        LOG.debug("IPSec config change id={} request={}", id, entry);
        final CompletionStage<IpsecSadAddDelEntryReply> ipsecSadEntryAddDellReplyFuture =
                getFutureJVpp().ipsecSadAddDelEntry(entry);
        getReplyForWrite(ipsecSadEntryAddDellReplyFuture.toCompletableFuture(), id);
        if (adding) {
            sadEntryMapping.addChild(dataAfter.key().getDirection().getName(), entry.sadId,
                    String.valueOf(dataAfter.key().getSpi()), writeContext.getMappingContext());
        } else {
            sadEntryMapping
                    .removeChild(dataAfter.key().getDirection().getName(), String.valueOf(dataAfter.key().getSpi()),
                            writeContext.getMappingContext());
        }
    }

    private void fillAhAuthentication(IpsecSadAddDelEntry targetEntry, Ah data) {
        //0 = None, 1 = MD5-96, 2 = SHA1-96, 3 = SHA-256, 4 = SHA-384, 5=SHA-512
        AuthenticationAlgorithm authAlg = data.getAuthenticationAlgorithm();
        if (authAlg != null) {
            String integKey;
            if (authAlg instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacMd596) {
                integKey =
                        ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacMd596) authAlg)
                                .getHmacMd596().getKeyStr().stringValue();
                targetEntry.integrityAlgorithm = 1;
            } else if (authAlg instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacSha196) {
                integKey =
                        ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacSha196) authAlg)
                                .getHmacSha196().getKeyStr().stringValue();
                targetEntry.integrityAlgorithm = 2;
            } else {
                targetEntry.integrityAlgorithm = 0;
                return;
            }
            targetEntry.integrityKey = integKey.getBytes();
        }
    }

    private void fillEspAuthentication(IpsecSadAddDelEntry targetEntry, Esp data) {
        //0 = None, 1 = MD5-96, 2 = SHA1-96, 3 = SHA-256, 4 = SHA-384, 5=SHA-512
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.Authentication
                authAlg = data.getAuthentication();
        if (authAlg != null) {
            String integKey;
            if (authAlg.getAuthenticationAlgorithm() instanceof HmacMd596) {
                integKey = ((HmacMd596) authAlg.getAuthenticationAlgorithm()).getHmacMd596().getKeyStr().stringValue();
                targetEntry.integrityAlgorithm = 1;
            } else if (authAlg.getAuthenticationAlgorithm() instanceof HmacSha196) {
                integKey =
                        ((HmacSha196) authAlg.getAuthenticationAlgorithm()).getHmacSha196().getKeyStr().stringValue();
                targetEntry.integrityAlgorithm = 2;
            } else {
                targetEntry.integrityAlgorithm = 0;
                return;
            }
            targetEntry.integrityKey = integKey.getBytes();
        }
    }

    private void fillEspEncryption(IpsecSadAddDelEntry targetEntry, Esp data) {
        //0 = Null, 1 = AES-CBC-128, 2 = AES-CBC-192, 3 = AES-CBC-256, 4 = 3DES-CBC
        if (data.getEncryption() != null && data.getEncryption().getEncryptionAlgorithm() != null) {
            String cryptoKey = "";
            EncryptionAlgorithm encrAlg = data.getEncryption().getEncryptionAlgorithm();
            if (encrAlg instanceof Aes128Cbc) {
                cryptoKey = ((Aes128Cbc) encrAlg).getAes128Cbc().getKeyStr().stringValue();
                targetEntry.cryptoAlgorithm = 1;
            } else if (encrAlg instanceof Aes192Cbc) {
                cryptoKey = ((Aes192Cbc) encrAlg).getAes192Cbc().getKeyStr().stringValue();
                targetEntry.cryptoAlgorithm = 2;
            } else if (encrAlg instanceof Aes256Cbc) {
                cryptoKey = ((Aes256Cbc) encrAlg).getAes256Cbc().getKeyStr().stringValue();
                targetEntry.cryptoAlgorithm = 3;
            } else if (encrAlg instanceof DesCbc) {
                cryptoKey = ((DesCbc) encrAlg).getDesCbc().getKeyStr().stringValue();
                targetEntry.cryptoAlgorithm = 4;
            } else {
                targetEntry.cryptoAlgorithm = 0;
                return;
            }
            targetEntry.cryptoKey = cryptoKey.getBytes();
        }
    }

    private void fillAddresses(IpsecSadAddDelEntry targetEntry, SadEntries data) {
        if (data.getSourceAddress() != null && data.getSourceAddress().getIpAddress() != null) {
            IpAddress sourceAddr = data.getSourceAddress().getIpAddress();
            if (sourceAddr instanceof Ipv4Address) {
                Ipv4Address ipv4 = (Ipv4Address) sourceAddr;
                targetEntry.isTunnelIpv6 = 0;
                targetEntry.tunnelSrcAddress = ipv4AddressNoZoneToArray(ipv4.getIpv4Address().getValue());
            } else if (sourceAddr instanceof Ipv6Address) {
                Ipv6Address ipv6 = (Ipv6Address) sourceAddr;
                targetEntry.isTunnelIpv6 = 1;
                targetEntry.tunnelSrcAddress = ipv6AddressNoZoneToArray(ipv6.getIpv6Address());
            }
        }

        if (data.getDestinationAddress() != null && data.getDestinationAddress().getIpAddress() != null) {
            IpAddress destAddr = data.getDestinationAddress().getIpAddress();

            if (destAddr instanceof Ipv4Address) {
                Ipv4Address ipv4 = (Ipv4Address) destAddr;
                targetEntry.isTunnelIpv6 = 0;
                targetEntry.tunnelDstAddress = ipv4AddressNoZoneToArray(ipv4.getIpv4Address().getValue());
            } else if (destAddr instanceof Ipv6Address) {
                Ipv6Address ipv6 = (Ipv6Address) destAddr;
                targetEntry.isTunnelIpv6 = 1;
                targetEntry.tunnelDstAddress = ipv6AddressNoZoneToArray(ipv6.getIpv6Address());
            }
        }
    }
}
