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
import io.fd.vpp.jvpp.core.dto.IpsecSadEntryAddDel;
import io.fd.vpp.jvpp.core.dto.IpsecSadEntryAddDelReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import io.fd.vpp.jvpp.core.types.IpsecCryptoAlg;
import io.fd.vpp.jvpp.core.types.IpsecIntegAlg;
import io.fd.vpp.jvpp.core.types.IpsecProto;
import io.fd.vpp.jvpp.core.types.IpsecSadEntry;
import io.fd.vpp.jvpp.core.types.IpsecSadFlags;
import io.fd.vpp.jvpp.core.types.Key;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecSadEntriesAugmentation;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecMode;
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
        final IpsecSadEntryAddDel request = new IpsecSadEntryAddDel();
        request.entry = new IpsecSadEntry();
        IpsecSadEntriesAugmentation augment = dataAfter.augmentation(IpsecSadEntriesAugmentation.class);
        if (augment != null && augment.getSaId() != null) {
            request.entry.sadId = augment.getSaId();
        }
        if (dataAfter.getSpi() != null) {
            request.entry.spi = dataAfter.getSpi().intValue();
        }
        request.entry.flags = new IpsecSadFlags();
        if (dataAfter.getAntiReplayWindow() != null && dataAfter.getAntiReplayWindow() > 0) {
            request.entry.flags.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_USE_ANTI_REPLAY);
        }
        if (dataAfter.getSaMode() != null && dataAfter.getSaMode().equals(IpsecMode.Tunnel)) {
            if (dataAfter.getSourceAddress() != null &&
                    dataAfter.getSourceAddress().getIpAddress() instanceof Ipv4Address) {
                request.entry.flags.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_IS_TUNNEL);
            } else if (dataAfter.getSourceAddress() != null &&
                    dataAfter.getSourceAddress().getIpAddress() instanceof Ipv6Address) {
                request.entry.flags.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_IS_TUNNEL_V6);
            }
        }
        request.isAdd = adding
                ? ByteDataTranslator.BYTE_TRUE
                : ByteDataTranslator.BYTE_FALSE;
        if (dataAfter.getEsp() != null) {
            request.entry.protocol = IpsecProto.IPSEC_API_PROTO_ESP;
            fillEspAuthentication(request, dataAfter.getEsp());
            fillEspEncryption(request, dataAfter.getEsp());

        } else if (dataAfter.getAh() != null) {
            request.entry.protocol = IpsecProto.IPSEC_API_PROTO_AH;
            fillAhAuthentication(request, dataAfter.getAh());
            fillAhEncryption(request, dataAfter.getAh());
        }

        fillAddresses(request, dataAfter);

        LOG.debug("IPSec config change id={} request={}", id, request);
        final CompletionStage<IpsecSadEntryAddDelReply> ipsecSadEntryAddDellReplyFuture =
                getFutureJVpp().ipsecSadEntryAddDel(request);
        getReplyForWrite(ipsecSadEntryAddDellReplyFuture.toCompletableFuture(), id);
        if (adding) {
            sadEntryMapping.addChild(dataAfter.key().getDirection().getName(), request.entry.sadId,
                    String.valueOf(dataAfter.key().getSpi()), writeContext.getMappingContext());
        } else {
            sadEntryMapping
                    .removeChild(dataAfter.key().getDirection().getName(), String.valueOf(dataAfter.key().getSpi()),
                            writeContext.getMappingContext());
        }
    }

    private void fillAhAuthentication(IpsecSadEntryAddDel request, Ah data) {
        //0 = None, 1 = MD5-96, 2 = SHA1-96, 3 = SHA-256, 4 = SHA-384, 5=SHA-512
        AuthenticationAlgorithm authAlg = data.getAuthenticationAlgorithm();
        if (authAlg != null) {
            String integKey;
            if (authAlg instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacMd596) {
                integKey =
                        ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacMd596) authAlg)
                                .getHmacMd596().getKeyStr().stringValue();
                request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_MD5_96;
            } else if (authAlg instanceof org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacSha196) {
                integKey =
                        ((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacSha196) authAlg)
                                .getHmacSha196().getKeyStr().stringValue();
                request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_SHA1_96;
            } else {
                request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_NONE;
                return;
            }
            request.entry.integrityKey = new Key();
            request.entry.integrityKey.data = integKey.getBytes();
            request.entry.integrityKey.length = (byte) integKey.getBytes().length;
            request.entry.cryptoKey = new Key();
            request.entry.cryptoKey.data = null;
            request.entry.cryptoKey.length = 0 ;
        }
    }

    private void fillAhEncryption(IpsecSadEntryAddDel request, Ah data) {
        request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_NONE;
        request.entry.cryptoKey = new Key();
        request.entry.cryptoKey.data = null;
        request.entry.cryptoKey.length = 0;
    }

    private void fillEspAuthentication(IpsecSadEntryAddDel request, Esp data) {
        //0 = None, 1 = MD5-96, 2 = SHA1-96, 3 = SHA-256, 4 = SHA-384, 5=SHA-512
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.Authentication
                authAlg = data.getAuthentication();
        if (authAlg != null) {
            String integKey;
            if (authAlg.getAuthenticationAlgorithm() instanceof HmacMd596) {
                integKey = ((HmacMd596) authAlg.getAuthenticationAlgorithm()).getHmacMd596().getKeyStr().stringValue();
                request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_MD5_96;
            } else if (authAlg.getAuthenticationAlgorithm() instanceof HmacSha196) {
                integKey =
                        ((HmacSha196) authAlg.getAuthenticationAlgorithm()).getHmacSha196().getKeyStr().stringValue();
                request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_SHA1_96;
            } else {
                request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_NONE;
                return;
            }
            request.entry.integrityKey = new Key();
            request.entry.integrityKey.data = integKey.getBytes();
            request.entry.integrityKey.length = (byte) integKey.getBytes().length;
        }
    }

    private void fillEspEncryption(IpsecSadEntryAddDel request, Esp data) {
        //0 = Null, 1 = AES-CBC-128, 2 = AES-CBC-192, 3 = AES-CBC-256, 4 = 3DES-CBC
        if (data.getEncryption() != null && data.getEncryption().getEncryptionAlgorithm() != null) {
            String cryptoKey = "";
            EncryptionAlgorithm encrAlg = data.getEncryption().getEncryptionAlgorithm();
            if (encrAlg instanceof Aes128Cbc) {
                cryptoKey = ((Aes128Cbc) encrAlg).getAes128Cbc().getKeyStr().stringValue();
                request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_AES_CBC_128;
            } else if (encrAlg instanceof Aes192Cbc) {
                cryptoKey = ((Aes192Cbc) encrAlg).getAes192Cbc().getKeyStr().stringValue();
                request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_AES_CBC_192;
            } else if (encrAlg instanceof Aes256Cbc) {
                cryptoKey = ((Aes256Cbc) encrAlg).getAes256Cbc().getKeyStr().stringValue();
                request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_AES_CBC_256;
            } else if (encrAlg instanceof DesCbc) {
                cryptoKey = ((DesCbc) encrAlg).getDesCbc().getKeyStr().stringValue();
                // TODO verify before the value was "4" now the result is "10"
                request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_DES_CBC;
            } else {
                request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_NONE;
                return;
            }
            request.entry.cryptoKey = new Key();
            request.entry.cryptoKey.data = cryptoKey.getBytes();
            request.entry.cryptoKey.length = (byte) cryptoKey.getBytes().length;
        }
    }

    private void fillAddresses(IpsecSadEntryAddDel request, SadEntries data) {
        if (data.getSourceAddress() != null && data.getSourceAddress().getIpAddress() != null) {
            IpAddress sourceAddr = data.getSourceAddress().getIpAddress();
            if (sourceAddr instanceof Ipv4Address) {
                Ipv4Address ipv4 = (Ipv4Address) sourceAddr;
                request.entry.tunnelSrc = ipv4AddressToAddress(ipv4.getIpv4Address());
            } else if (sourceAddr instanceof Ipv6Address) {
                Ipv6Address ipv6 = (Ipv6Address) sourceAddr;
                request.entry.tunnelSrc = ipv6AddressToAddress(ipv6.getIpv6Address());
            }
        }

        if (data.getDestinationAddress() != null && data.getDestinationAddress().getIpAddress() != null) {
            IpAddress destAddr = data.getDestinationAddress().getIpAddress();

            if (destAddr instanceof Ipv4Address) {
                Ipv4Address ipv4 = (Ipv4Address) destAddr;
                request.entry.tunnelDst = ipv4AddressToAddress(ipv4.getIpv4Address());
            } else if (destAddr instanceof Ipv6Address) {
                Ipv6Address ipv6 = (Ipv6Address) destAddr;
                request.entry.tunnelDst = ipv6AddressToAddress(ipv6.getIpv6Address());
            }
        }
    }
}
