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
import java.util.LinkedList;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ipsec.sa.state.grouping.Sa;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IpsecStateCustomizerTest extends ReaderCustomizerTest<IpsecState, IpsecStateBuilder>
        implements ByteDataTranslator, Ipv4Translator, Ipv6Translator {

    private static InstanceIdentifier<IpsecState> IPSEC_STATE_ID = InstanceIdentifier.create(IpsecState.class);
    private static final String LOCAL_ADDR_START = "192.168.11.1";
    private static final String REMOTE_ADDR_START = "192.168.22.1";
    private static final String TUNNEL_SRC_ADDR = LOCAL_ADDR_START;
    private static final String TUNNEL_DST_ADDR = REMOTE_ADDR_START;
    private static final int REPLY_WINDOW = 88;
    private static final int SA_ID = 10;
    private static final int SPI = 1001;
    private static final int CRYPTO_ALG = 1;
    private static final String CRYPTO_KEY = "123456789";
    private static final int INTEG_ALG = 2;
    private static final String INTEG_KEY = "987654321";
    private static final int PROTOCOL = 1;
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
    protected void setUp() throws Exception {
        final IpsecSaDetailsReplyDump saDetailsReply = new IpsecSaDetailsReplyDump();
        LinkedList<IpsecSaDetails> saDetails = new LinkedList<>();
        IpsecSaDetails saDetail = new IpsecSaDetails();
        saDetail.spi = SPI;
        saDetail.saId = SA_ID;
        saDetail.cryptoAlg = CRYPTO_ALG;
        saDetail.cryptoKey = CRYPTO_KEY.getBytes();
        saDetail.integAlg = INTEG_ALG;
        saDetail.integKey = INTEG_KEY.getBytes();
        saDetail.isTunnel = BYTE_TRUE;
        saDetail.isTunnelIp6 = BYTE_FALSE;
        saDetail.protocol = PROTOCOL;
        saDetail.lastSeqInbound = LAST_SEQ_INB;
        saDetail.replayWindow = REPLY_WINDOW;
        saDetail.useAntiReplay = BYTE_TRUE;
        saDetail.tunnelSrcAddr = ipv4AddressNoZoneToArray(TUNNEL_SRC_ADDR);
        saDetail.tunnelDstAddr = ipv4AddressNoZoneToArray(TUNNEL_DST_ADDR);
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
        assertEquals(sa.getEncryptionAlgorithm().getIntValue(), CRYPTO_ALG);
        assertEquals(sa.getSpi().intValue(), SPI);
    }

    @Test
    public void testMerge() throws Exception {
        final IpsecStateBuilder parentBuilder = new IpsecStateBuilder();
        final IpsecStateBuilder builderForNewdata = new IpsecStateBuilder();
        builderForNewdata.setHoldDown(new Long(HOLD_DOWN));
        getCustomizer().merge(parentBuilder, builderForNewdata.build());
        assertEquals(parentBuilder.getHoldDown().intValue(), HOLD_DOWN);
    }
}
