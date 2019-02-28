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
import io.fd.jvpp.core.dto.IpsecSpdDetails;
import io.fd.jvpp.core.dto.IpsecSpdDetailsReplyDump;
import io.fd.jvpp.core.dto.IpsecSpdsDetails;
import io.fd.jvpp.core.dto.IpsecSpdsDetailsReplyDump;
import io.fd.jvpp.core.types.IpsecSpdAction;
import io.fd.jvpp.core.types.IpsecSpdEntry;
import java.util.LinkedList;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecStateSpdAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecStateSpdAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.Spd;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.SpdBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.SpdKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ipsec.state.spd.SpdEntries;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IpsecState;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class IpsecStateSpdCustomizerTest extends ReaderCustomizerTest<Spd, SpdBuilder>
        implements ByteDataTranslator, Ipv4Translator, Ipv6Translator {

    private static InstanceIdentifier<Spd> SPD_IID = InstanceIdentifier.create(IpsecState.class)
            .augmentation(IpsecStateSpdAugmentation.class).child(Spd.class, new SpdKey(10));

    private static final Ipv4Address LOCAL_ADDR_START = new Ipv4Address("192.168.11.1");
    private static final Ipv4Address LOCAL_ADDR_END = new Ipv4Address("192.168.11.255");
    private static final short PORT_START = 0;
    private static final short PORT_END = Short.MAX_VALUE;
    private static final int SPD_ID = 10;
    private static final int SA_ID = 10;
    private static final int PROTOCOL = 1;
    private static final int PRIORITY = 100;

    public IpsecStateSpdCustomizerTest() {
        super(Spd.class, SpdBuilder.class);
    }

    @Override
    protected ReaderCustomizer<Spd, SpdBuilder> initCustomizer() {
        return new IpsecStateSpdCustomizer(api);
    }

    @Override
    protected void setUp() throws Exception {
        final IpsecSpdDetailsReplyDump spdDetailsReply = new IpsecSpdDetailsReplyDump();
        LinkedList<IpsecSpdDetails> spdDetails = new LinkedList<>();
        IpsecSpdDetails spdDetail = new IpsecSpdDetails();
        spdDetail.entry = new IpsecSpdEntry();
        spdDetail.entry.isOutbound = BYTE_TRUE;
        spdDetail.entry.spdId = SPD_ID;
        spdDetail.entry.protocol = PROTOCOL;
        spdDetail.entry.localAddressStart = ipv4AddressToAddress(LOCAL_ADDR_START);
        spdDetail.entry.localAddressStop = ipv4AddressToAddress(LOCAL_ADDR_END);
        spdDetail.entry.localPortStart = PORT_START;
        spdDetail.entry.localPortStop = PORT_END;
        spdDetail.entry.policy = IpsecSpdAction.IPSEC_API_SPD_ACTION_PROTECT;
        spdDetail.entry.saId = SA_ID;
        spdDetail.entry.priority = PRIORITY;
        spdDetails.add(spdDetail);
        spdDetailsReply.ipsecSpdDetails = spdDetails;
        when(api.ipsecSpdDump(any())).thenReturn(future(spdDetailsReply));

        IpsecSpdsDetailsReplyDump spdsReply = new IpsecSpdsDetailsReplyDump();
        IpsecSpdsDetails spdsDetail = new IpsecSpdsDetails();
        spdsDetail.spdId = SPD_ID;
        spdsReply.ipsecSpdsDetails.add(spdsDetail);
        when(api.ipsecSpdsDump(any())).thenReturn(future(spdsReply));
    }

    @Test
    public void testReadSpd() throws ReadFailedException {
        final SpdBuilder builder = new SpdBuilder();
        getCustomizer().readCurrentAttributes(SPD_IID, builder, ctx);
        assertEquals(builder.getSpdEntries().size(), 1);
        SpdEntries spdEntries = builder.getSpdEntries().get(0);
        assertEquals(spdEntries.getDirection().getName(), "outbound");
        assertEquals(spdEntries.getPriority().intValue(), PRIORITY);
    }

    @Test
    public void testMerge() throws Exception {
        final IpsecStateSpdAugmentationBuilder parentBuilder = new IpsecStateSpdAugmentationBuilder();
        final IpsecStateSpdAugmentationBuilder builderForNewData = new IpsecStateSpdAugmentationBuilder();
        SpdBuilder spdBuilder = new SpdBuilder();
        spdBuilder.setSpdId(SPD_ID);
        getCustomizer().merge(parentBuilder, spdBuilder.build());
        assertEquals(parentBuilder.getSpd().size(), 1);
        assertEquals(parentBuilder.getSpd().get(0).getSpdId().intValue(), SPD_ID);
    }
}
