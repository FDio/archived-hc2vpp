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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.MultiNamingContext;
import io.fd.hc2vpp.ipsec.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.IpsecSadEntryAddDel;
import io.fd.jvpp.core.dto.IpsecSadEntryAddDelReply;
import io.fd.jvpp.core.types.IpsecCryptoAlg;
import io.fd.jvpp.core.types.IpsecIntegAlg;
import io.fd.jvpp.core.types.IpsecProto;
import io.fd.jvpp.core.types.IpsecSadEntry;
import io.fd.jvpp.core.types.IpsecSadFlags;
import io.fd.jvpp.core.types.Key;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IkeEncryptionAlgorithmT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IkeIntegrityAlgorithmT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.Ipsec;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecTrafficDirection;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.KeyStringGrouping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.Sad;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.AhBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.AuthenticationAlgorithm;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.EspBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.Authentication;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.AuthenticationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.Encryption;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.EncryptionBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.authentication.authentication.algorithm.HmacMd596Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.authentication.authentication.algorithm.HmacSha196Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.Aes128CbcBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.Aes192CbcBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.Aes256CbcBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.DesCbcBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sad.SadEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sad.SadEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sad.SadEntriesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class IpsecSadEntryCustomizerTest extends WriterCustomizerTest implements SchemaContextTestHelper,
        ByteDataTranslator, Ipv4Translator, Ipv6Translator {

    private static final String SAD_PATH = "/hc2vpp-ietf-ipsec:ipsec/hc2vpp-ietf-ipsec:sad";
    private static final InstanceIdentifier<Sad> SAD_IID =
            InstanceIdentifier.create(Ipsec.class).child(Sad.class);
    private static final String INTEG_KEY = "0123456789012346";
    private static final String CRYPTO_KEY = "9876543210987654";
    private static final Ipv4Address TNL_SRC_ADDR = new Ipv4Address("192.168.1.1");
    private static final Ipv4Address TNL_DST_ADDR = new Ipv4Address("192.168.1.2");
    private static final int SPI_1002 = 1002;
    private static final int SAD_ID = 10;

    private IpsecSadEntryCustomizer customizer;
    @Mock
    private MultiNamingContext namingCntext;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new IpsecSadEntryCustomizer(api, namingCntext);
        when(api.ipsecSadEntryAddDel(any())).thenReturn(future(new IpsecSadEntryAddDelReply()));
    }

    @Test
    public void testFlags() {
        IpsecSadFlags flags = new IpsecSadFlags();
        flags.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_IS_TUNNEL);
        flags.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_USE_ANTI_REPLAY);
        flags.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_USE_EXTENDED_SEQ_NUM);

        IpsecSadFlags flags2 = new IpsecSadFlags();
        flags2.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_USE_ANTI_REPLAY);
        flags2.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_USE_EXTENDED_SEQ_NUM);
        flags2.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_IS_TUNNEL);
        ;
        IpsecSadFlags flags3 = new IpsecSadFlags();
        flags3.setOptionsValue(7);

        assertEquals(4, IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_IS_TUNNEL.value);
        assertEquals(flags, flags2);
        assertEquals(7, flags.getOptionsValue());
        assertEquals(flags, flags3);
    }

    @Test
    public void testWrite(@InjectTestData(resourcePath = "/sadEntries/addDelSadEntry.json", id = SAD_PATH) Sad sad)
            throws WriteFailedException {
        final SadEntries data = sad.getSadEntries().get(0);
        final IpsecSadEntryAddDel request = new IpsecSadEntryAddDel();
        request.isAdd = BYTE_TRUE;
        request.entry = new io.fd.jvpp.core.types.IpsecSadEntry();
        request.entry.spi = SPI_1002;
        request.entry.sadId = SAD_ID;
        request.entry.integrityKey = new Key();
        request.entry.integrityKey.data = INTEG_KEY.getBytes();
        request.entry.integrityKey.length = (byte) INTEG_KEY.getBytes().length;
        request.entry.cryptoKey = new Key();
        request.entry.cryptoKey.data = CRYPTO_KEY.getBytes();
        request.entry.cryptoKey.length = (byte) CRYPTO_KEY.getBytes().length;
        request.entry.flags = new IpsecSadFlags();
        request.entry.flags.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_IS_TUNNEL);
        request.entry.tunnelSrc = ipv4AddressToAddress(TNL_SRC_ADDR);
        request.entry.tunnelDst = ipv4AddressToAddress(TNL_DST_ADDR);

        // ESP
        request.entry.protocol = IpsecProto.IPSEC_API_PROTO_ESP;
        // - auth MD5-96
        //  - crypto Aes-Cbc-128
        testEspAuthEncrCombination(data, IkeIntegrityAlgorithmT.AuthHmacMd596,
                IkeEncryptionAlgorithmT.EncrAesCbc128, request);
        //  - crypto Aes-Cbc-192
        testEspAuthEncrCombination(data, IkeIntegrityAlgorithmT.AuthHmacMd596,
                IkeEncryptionAlgorithmT.EncrAesCbc192, request);
        //  - crypto Aes-Cbc-256
        testEspAuthEncrCombination(data, IkeIntegrityAlgorithmT.AuthHmacMd596,
                IkeEncryptionAlgorithmT.EncrAesCbc256, request);
        //  - crypto DesCbc
        testEspAuthEncrCombination(data, IkeIntegrityAlgorithmT.AuthHmacMd596,
                IkeEncryptionAlgorithmT.EncrDes, request);

        // - auth SHA1-96
        //  - crypto Aes-Cbc-128
        testEspAuthEncrCombination(data, IkeIntegrityAlgorithmT.AuthHmacSha196,
                IkeEncryptionAlgorithmT.EncrAesCbc128, request);
        //  - crypto Aes-Cbc-192
        testEspAuthEncrCombination(data, IkeIntegrityAlgorithmT.AuthHmacSha196,
                IkeEncryptionAlgorithmT.EncrAesCbc192, request);
        //  - crypto Aes-Cbc-256
        testEspAuthEncrCombination(data, IkeIntegrityAlgorithmT.AuthHmacSha196,
                IkeEncryptionAlgorithmT.EncrAesCbc256, request);
        //  - crypto DesCbc
        testEspAuthEncrCombination(data, IkeIntegrityAlgorithmT.AuthHmacSha196,
                IkeEncryptionAlgorithmT.EncrDes, request);

        // AH
        request.entry.protocol = IpsecProto.IPSEC_API_PROTO_AH;
        request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_NONE;
        request.entry.cryptoKey = new Key();
        request.entry.cryptoKey.data = null;
        request.entry.cryptoKey.length = 0;
        // - auth SHA1-96
        testAhAuthorization(data, IkeIntegrityAlgorithmT.AuthHmacSha196, request);
        // - auth MD5-96
        testAhAuthorization(data, IkeIntegrityAlgorithmT.AuthHmacMd596, request);
    }

    @Test
    public void testUpdate(
            @InjectTestData(resourcePath = "/sadEntries/addDelSadEntry_Ipv6_before.json", id = SAD_PATH) Sad relaysBefore,
            @InjectTestData(resourcePath = "/sadEntries/addDelSadEntry_Ipv6_after.json", id = SAD_PATH) Sad relayAfter)
            throws WriteFailedException {
        final SadEntries before = relaysBefore.getSadEntries().get(0);
        final SadEntries after = relayAfter.getSadEntries().get(0);
        final Long spi = after.getSpi();
        customizer.updateCurrentAttributes(getId(IpsecTrafficDirection.Outbound, spi), before, after, writeContext);
        final IpsecSadEntryAddDel request = new IpsecSadEntryAddDel();
        request.isAdd = BYTE_TRUE;
        request.entry = new IpsecSadEntry();
        request.entry.spi = SPI_1002;
        request.entry.sadId = SAD_ID;
        request.entry.protocol = IpsecProto.IPSEC_API_PROTO_AH;
        request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_MD5_96;
        request.entry.integrityKey = new Key();
        request.entry.integrityKey.data = INTEG_KEY.getBytes();
        request.entry.integrityKey.length = (byte) INTEG_KEY.getBytes().length;
        request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_NONE;
        request.entry.cryptoKey = new Key();
        request.entry.cryptoKey.data = null;
        request.entry.cryptoKey.length = 0;
        request.entry.flags = new IpsecSadFlags();
        request.entry.flags.add(IpsecSadFlags.IpsecSadFlagsOptions.IPSEC_API_SAD_FLAG_USE_ANTI_REPLAY);
        request.entry.tunnelSrc = ipv6AddressToAddress(Ipv6Address.getDefaultInstance("2001::11"));
        request.entry.tunnelDst = ipv6AddressToAddress(Ipv6Address.getDefaultInstance("2001::12"));
        verify(api).ipsecSadEntryAddDel(request);
    }

    @Test
    public void testDelete(@InjectTestData(resourcePath = "/sadEntries/delSadEntry.json", id = SAD_PATH) Sad sad)
            throws WriteFailedException {
        final SadEntries data = sad.getSadEntries().get(0);
        final Long spi = data.getSpi();
        customizer.deleteCurrentAttributes(getId(IpsecTrafficDirection.Outbound, spi), data, writeContext);
        final IpsecSadEntryAddDel request = new IpsecSadEntryAddDel();
        request.isAdd = BYTE_FALSE;
        request.entry = new IpsecSadEntry();
        request.entry.spi = SPI_1002;
        request.entry.sadId = SAD_ID;
        request.entry.flags = new IpsecSadFlags();
        verify(api).ipsecSadEntryAddDel(request);
    }

    private InstanceIdentifier<SadEntries> getId(final IpsecTrafficDirection direction, final Long spi) {
        return SAD_IID.child(SadEntries.class, new SadEntriesKey(direction, spi));
    }

    private void testAhAuthorization(final SadEntries otherData, final IkeIntegrityAlgorithmT authAlg,
                                     final IpsecSadEntryAddDel request) throws WriteFailedException {
        SadEntriesBuilder builder = new SadEntriesBuilder(otherData);
        builder.setEsp(null);
        AhBuilder ahBuilder = new AhBuilder();
        ahBuilder.setAuthenticationAlgorithm(getAhAuthentication(authAlg));
        builder.setAh(ahBuilder.build());
        customizer.writeCurrentAttributes(getId(IpsecTrafficDirection.Outbound, Integer.toUnsignedLong(SPI_1002)),
                builder.build(), writeContext);
        verify(api).ipsecSadEntryAddDel(request);
    }

    private void testEspAuthEncrCombination(final SadEntries otherData, final IkeIntegrityAlgorithmT authAlg,
                                            final IkeEncryptionAlgorithmT encrAlg, final IpsecSadEntryAddDel request)
            throws WriteFailedException {
        SadEntriesBuilder builder = new SadEntriesBuilder(otherData);
        builder.setAh(null);
        EspBuilder espBuilder = new EspBuilder();
        espBuilder.setAuthentication(getEspAuthentication(authAlg))
                .setEncryption(getEspEncryption(encrAlg));
        builder.setEsp(espBuilder.build());
        customizer.writeCurrentAttributes(getId(IpsecTrafficDirection.Outbound, Integer.toUnsignedLong(SPI_1002)),
                builder.build(), writeContext);

        if (encrAlg == IkeEncryptionAlgorithmT.EncrAesCbc128) {
            request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_AES_CBC_128;
        } else if (encrAlg == IkeEncryptionAlgorithmT.EncrAesCbc192) {
            request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_AES_CBC_192;
        } else if (encrAlg == IkeEncryptionAlgorithmT.EncrAesCbc256) {
            request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_AES_CBC_256;
        } else if (encrAlg == IkeEncryptionAlgorithmT.EncrDes) {
            request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_DES_CBC;
        } else {
            request.entry.cryptoAlgorithm = IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_NONE;
        }

        if (authAlg == IkeIntegrityAlgorithmT.AuthHmacMd596) {
            request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_MD5_96;
        } else if (authAlg == IkeIntegrityAlgorithmT.AuthHmacSha196) {
            request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_SHA1_96;
        } else {
            request.entry.integrityAlgorithm = IpsecIntegAlg.IPSEC_API_INTEG_ALG_NONE;
        }

        verify(api).ipsecSadEntryAddDel(request);
    }

    private Encryption getEspEncryption(IkeEncryptionAlgorithmT alg) {
        if (alg == IkeEncryptionAlgorithmT.EncrAesCbc128) {
            return new EncryptionBuilder().setEncryptionAlgorithm(new Aes128CbcBuilder().
                    setAes128Cbc(
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.aes._128.cbc.Aes128CbcBuilder()
                                    .setKeyStr(new KeyStringGrouping.KeyStr(CRYPTO_KEY))
                                    .build()).build()).build();
        } else if (alg == IkeEncryptionAlgorithmT.EncrAesCbc192) {
            return new EncryptionBuilder().setEncryptionAlgorithm(new Aes192CbcBuilder().
                    setAes192Cbc(
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.aes._192.cbc.Aes192CbcBuilder()
                                    .setKeyStr(new KeyStringGrouping.KeyStr(CRYPTO_KEY))
                                    .build()).build()).build();
        } else if (alg == IkeEncryptionAlgorithmT.EncrAesCbc256) {
            return new EncryptionBuilder().setEncryptionAlgorithm(new Aes256CbcBuilder().
                    setAes256Cbc(
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.aes._256.cbc.Aes256CbcBuilder()
                                    .setKeyStr(new KeyStringGrouping.KeyStr(CRYPTO_KEY))
                                    .build()).build()).build();
        } else if (alg == IkeEncryptionAlgorithmT.EncrDes) {
            return new EncryptionBuilder().setEncryptionAlgorithm(new DesCbcBuilder().setDesCbc(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.encryption.encryption.algorithm.des.cbc.DesCbcBuilder()
                            .setKeyStr(new KeyStringGrouping.KeyStr(CRYPTO_KEY))
                            .build()).build()).build();
        }

        return null;
    }

    private Authentication getEspAuthentication(IkeIntegrityAlgorithmT alg) {
        if (alg == IkeIntegrityAlgorithmT.AuthHmacSha196) {
            return new AuthenticationBuilder().setAuthenticationAlgorithm(new HmacSha196Builder().setHmacSha196(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.authentication.authentication.algorithm.hmac.sha1._96.HmacSha196Builder()
                            .setKeyStr(new KeyStringGrouping.KeyStr(INTEG_KEY)).build()).build()).build();
        } else if (alg == IkeIntegrityAlgorithmT.AuthHmacMd596) {
            return new AuthenticationBuilder().setAuthenticationAlgorithm(new HmacMd596Builder().setHmacMd596(
                    new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.esp.grouping.esp.authentication.authentication.algorithm.hmac.md5._96.HmacMd596Builder()
                            .setKeyStr(new KeyStringGrouping.KeyStr(INTEG_KEY)).build()).build()).build();
        }
        return null;
    }

    private AuthenticationAlgorithm getAhAuthentication(IkeIntegrityAlgorithmT alg) {
        if (alg == IkeIntegrityAlgorithmT.AuthHmacSha196) {
            return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacSha196Builder()
                    .setHmacSha196(
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.hmac.sha1._96.HmacSha196Builder()
                                    .setKeyStr(new KeyStringGrouping.KeyStr(INTEG_KEY)).build()).build();
        } else if (alg == IkeIntegrityAlgorithmT.AuthHmacMd596) {
            return new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.HmacMd596Builder()
                    .setHmacMd596(
                            new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.ah.grouping.ah.authentication.algorithm.hmac.md5._96.HmacMd596Builder()
                                    .setKeyStr(new KeyStringGrouping.KeyStr(INTEG_KEY)).build()).build();
        }
        return null;
    }
}
