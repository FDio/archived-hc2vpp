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

package io.fd.hc2vpp.ipsec.read;

import static io.fd.vpp.jvpp.core.types.IpsecCryptoAlg.IPSEC_API_CRYPTO_ALG_AES_CBC_128;
import static io.fd.vpp.jvpp.core.types.IpsecIntegAlg.IPSEC_API_INTEG_ALG_SHA1_96;
import static io.fd.vpp.jvpp.core.types.IpsecProto.IPSEC_API_PROTO_ESP;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.IpsecSaDetails;
import io.fd.vpp.jvpp.core.dto.IpsecSaDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.IpsecSaDump;
import io.fd.vpp.jvpp.core.types.IpsecSadEntry;
import io.fd.vpp.jvpp.core.types.IpsecSadFlags;
import io.fd.vpp.jvpp.core.types.Key;
import java.util.LinkedList;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IkeEncryptionAlgorithmT;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.state.grouping.Sa;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IpsecStateCustomizerTest extends ReaderCustomizerTest<IpsecState, IpsecStateBuilder>
        implements ByteDataTranslator, Ipv4Translator, Ipv6Translator {

    private static InstanceIdentifier<IpsecState> IPSEC_STATE_ID = InstanceIdentifier.create(IpsecState.class);
    private static final Ipv4AddressNoZone TUNNEL_SRC_ADDR = new Ipv4AddressNoZone("192.168.11.1");
    private static final Ipv4AddressNoZone TUNNEL_DST_ADDR = new Ipv4AddressNoZone("192.168.22.1");
    private static final int REPLY_WINDOW = 88;
    private static final int SA_ID = 10;
    private static final int SPI = 1001;
    private static final String CRYPTO_KEY = "123456789";
    private static final int INTEG_ALG = 2;
    private static final String INTEG_KEY = "987654321";
    private static final int LAST_SEQ_INB = 8;
    private static final int HOLD_DOWN = 88;

    public IpsecStateCustomizerTest() {
        super(IpsecState.class, IpsecStateBuilder.class);
    }

    @Override
    protected ReaderCustomizer<IpsecState, IpsecStateBuilder> initCustomizer() {
        return new IpsecStateCustomizer(api);
    }

    @Override
    protected void setUp() {
        final IpsecSaDetailsReplyDump saDetailsReply = new IpsecSaDetailsReplyDump();
        LinkedList<IpsecSaDetails> saDetails = new LinkedList<>();
        IpsecSaDetails saDetail = new IpsecSaDetails();
        saDetail.entry = new IpsecSadEntry();
        saDetail.entry.spi = SPI;
        saDetail.entry.sadId = SA_ID;
        saDetail.entry.cryptoAlgorithm = IPSEC_API_CRYPTO_ALG_AES_CBC_128;
        saDetail.entry.cryptoKey = new Key();
        saDetail.entry.cryptoKey.data = CRYPTO_KEY.getBytes();
        saDetail.entry.cryptoKey.length = (byte) CRYPTO_KEY.getBytes().length;
        saDetail.entry.integrityAlgorithm = IPSEC_API_INTEG_ALG_SHA1_96;
        saDetail.entry.integrityKey = new Key();
        saDetail.entry.integrityKey.data = INTEG_KEY.getBytes();
        saDetail.entry.integrityKey.length = (byte) INTEG_KEY.getBytes().length;
        saDetail.entry.protocol = IPSEC_API_PROTO_ESP;
        saDetail.lastSeqInbound = LAST_SEQ_INB;
        saDetail.replayWindow = REPLY_WINDOW;
        saDetail.entry.flags = IpsecSadFlags.forValue(IpsecSadFlags.IPSEC_API_SAD_FLAG_IS_TUNNEL.value +
                IpsecSadFlags.IPSEC_API_SAD_FLAG_USE_ANTI_REPLAY.value);
        saDetail.entry.tunnelSrc = ipv4AddressNoZoneToAddress(TUNNEL_SRC_ADDR);
        saDetail.entry.tunnelDst = ipv4AddressNoZoneToAddress(TUNNEL_DST_ADDR);
        saDetails.add(saDetail);
        saDetailsReply.ipsecSaDetails = saDetails;
        IpsecSaDump saDump = new IpsecSaDump();
        saDump.saId = SA_ID;
        when(api.ipsecSaDump(any())).thenReturn(future(saDetailsReply));
    }

    @Test
    public void testReadSa() throws ReadFailedException {
        final IpsecStateBuilder builder = new IpsecStateBuilder();
        getCustomizer().readCurrentAttributes(IPSEC_STATE_ID, builder, ctx);
        assertEquals(builder.getSa().size(), 1);
        Sa sa = builder.getSa().get(0);
        assertEquals(sa.getAntiReplayWindow().intValue(), REPLY_WINDOW);
        assertEquals(sa.getAuthenticationAlgorithm().getIntValue(), INTEG_ALG);
        assertEquals(sa.getEncryptionAlgorithm(), IkeEncryptionAlgorithmT.EncrAesCbc128);
        assertEquals(sa.getSpi().intValue(), SPI);
    }

    @Test
    public void testMerge() {
        final IpsecStateBuilder parentBuilder = new IpsecStateBuilder();
        final IpsecStateBuilder builderForNewdata = new IpsecStateBuilder();
        builderForNewdata.setHoldDown((long) HOLD_DOWN);
        getCustomizer().merge(parentBuilder, builderForNewdata.build());
        assertEquals(parentBuilder.getHoldDown().intValue(), HOLD_DOWN);
    }
}
