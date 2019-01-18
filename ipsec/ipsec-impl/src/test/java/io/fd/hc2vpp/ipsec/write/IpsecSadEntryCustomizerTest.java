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
import io.fd.vpp.jvpp.core.dto.IpsecSadAddDelEntry;
import io.fd.vpp.jvpp.core.dto.IpsecSadAddDelEntryReply;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
    private static final String TNL_SRC_ADDR = "192.168.1.1";
    private static final String TNL_DST_ADDR = "192.168.1.2";
    private static final int SPI_1002 = 1002;
    private static final int SAD_ID = 10;

    private IpsecSadEntryCustomizer customizer;
    @Mock
    private MultiNamingContext namingCntext;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new IpsecSadEntryCustomizer(api, namingCntext);
        when(api.ipsecSadAddDelEntry(any())).thenReturn(future(new IpsecSadAddDelEntryReply()));
    }

    @Test
    public void testWrite(@InjectTestData(resourcePath = "/sadEntries/addDelSadEntry.json", id = SAD_PATH) Sad sad)
            throws WriteFailedException {
        final SadEntries data = sad.getSadEntries().get(0);
        final IpsecSadAddDelEntry request = new IpsecSadAddDelEntry();
        request.isAdd = BYTE_TRUE;
        request.spi = SPI_1002;
        request.sadId = SAD_ID;
        request.isTunnel = BYTE_TRUE;
        request.isTunnelIpv6 = BYTE_FALSE;
        request.integrityKey = INTEG_KEY.getBytes();
        request.integrityKeyLength = (byte) request.integrityKey.length;
        request.cryptoKey = CRYPTO_KEY.getBytes();
        request.cryptoKeyLength = (byte) request.cryptoKey.length;
        request.useAntiReplay = 0;
        request.tunnelSrcAddress = ipv4AddressNoZoneToArray(TNL_SRC_ADDR);
        request.tunnelDstAddress = ipv4AddressNoZoneToArray(TNL_DST_ADDR);

        // ESP
        request.protocol = BYTE_TRUE; //0 = AH, 1 = ESP
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
        request.protocol = BYTE_FALSE;
        request.cryptoAlgorithm = 0;
        request.cryptoKey = null;
        request.cryptoKeyLength = 0;
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
        final IpsecSadAddDelEntry request = new IpsecSadAddDelEntry();
        request.isAdd = BYTE_TRUE;
        request.spi = SPI_1002;
        request.sadId = SAD_ID;
        request.protocol = BYTE_FALSE;
        request.isTunnel = BYTE_FALSE;
        request.isTunnelIpv6 = BYTE_TRUE;
        request.integrityAlgorithm = 1;
        request.integrityKey = INTEG_KEY.getBytes();
        request.integrityKeyLength = (byte) request.integrityKey.length;
        request.useAntiReplay = BYTE_TRUE;
        request.tunnelSrcAddress = ipv6AddressNoZoneToArray(Ipv6Address.getDefaultInstance("2001::11"));
        request.tunnelDstAddress = ipv6AddressNoZoneToArray(Ipv6Address.getDefaultInstance("2001::12"));
        verify(api).ipsecSadAddDelEntry(request);
    }

    @Test
    public void testDelete(@InjectTestData(resourcePath = "/sadEntries/delSadEntry.json", id = SAD_PATH) Sad sad)
            throws WriteFailedException {
        final SadEntries data = sad.getSadEntries().get(0);
        final Long spi = data.getSpi();
        customizer.deleteCurrentAttributes(getId(IpsecTrafficDirection.Outbound, spi), data, writeContext);
        final IpsecSadAddDelEntry request = new IpsecSadAddDelEntry();
        request.isAdd = BYTE_FALSE;
        request.spi = SPI_1002;
        request.sadId = SAD_ID;
        verify(api).ipsecSadAddDelEntry(request);
    }

    private InstanceIdentifier<SadEntries> getId(final IpsecTrafficDirection direction, final Long spi) {
        return SAD_IID.child(SadEntries.class, new SadEntriesKey(direction, spi));
    }

    private void testAhAuthorization(final SadEntries otherData, final IkeIntegrityAlgorithmT authAlg,
                                     final IpsecSadAddDelEntry request) throws WriteFailedException {
        SadEntriesBuilder builder = new SadEntriesBuilder(otherData);
        builder.setEsp(null);
        AhBuilder ahBuilder = new AhBuilder();
        ahBuilder.setAuthenticationAlgorithm(getAhAuthentication(authAlg));
        builder.setAh(ahBuilder.build());
        customizer.writeCurrentAttributes(getId(IpsecTrafficDirection.Outbound, Integer.toUnsignedLong(SPI_1002)),
                builder.build(), writeContext);
        verify(api).ipsecSadAddDelEntry(request);
    }

    private void testEspAuthEncrCombination(final SadEntries otherData, final IkeIntegrityAlgorithmT authAlg,
                                            final IkeEncryptionAlgorithmT encrAlg, final IpsecSadAddDelEntry request)
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
            request.cryptoAlgorithm = 1;
        } else if (encrAlg == IkeEncryptionAlgorithmT.EncrAesCbc192) {
            request.cryptoAlgorithm = 2;
        } else if (encrAlg == IkeEncryptionAlgorithmT.EncrAesCbc256) {
            request.cryptoAlgorithm = 3;
        } else if (encrAlg == IkeEncryptionAlgorithmT.EncrDes) {
            request.cryptoAlgorithm = 4;
        } else {
            request.cryptoAlgorithm = 0;
        }

        if (authAlg == IkeIntegrityAlgorithmT.AuthHmacMd596) {
            request.integrityAlgorithm = 1;
        } else if (authAlg == IkeIntegrityAlgorithmT.AuthHmacSha196) {
            request.integrityAlgorithm = 2;
        } else {
            request.integrityAlgorithm = 0;
        }

        verify(api).ipsecSadAddDelEntry(request);
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
