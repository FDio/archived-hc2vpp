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

package io.fd.hc2vpp.v3po.interfacesstate;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsDumpManager;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsSample;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.VnetPerInterfaceCombinedCounters;
import io.fd.vpp.jvpp.core.types.VnetCombinedCounter;
import java.math.BigInteger;
import java.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.Statistics;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state._interface.StatisticsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceStatisticsCustomizerTest extends ReaderCustomizerTest<Statistics, StatisticsBuilder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String IF_NAME = "tap";
    private static final int RX_BROADCAST_P = 8;
    private static final int RX_MULTICAST_P = 7;
    private static final int RX_UNICAST_P = 6;
    private static final int RX_BYTES = 1000;
    private static final int TX_BROADCAST_P = 88;
    private static final int TX_MULTICAST_P = 77;
    private static final int TX_UNICAST_P = 66;
    private static final int TX_BYTES = 1100;

    private NamingContext namingContext;
    private static final InstanceIdentifier<Statistics> IID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .child(Statistics.class);
    @Mock
    private InterfaceCacheStatisticsDumpManager statManager;


    public InterfaceStatisticsCustomizerTest() {
        super(Statistics.class, InterfaceBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        namingContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, 1, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<Statistics, StatisticsBuilder> initCustomizer() {
        return new InterfaceStatisticsCustomizer(namingContext, statManager);
    }

    @Test
    public void testRead() throws ReadFailedException {
        when(statManager.getStatisticsData(anyInt())).thenReturn(
                (new InterfaceCacheStatisticsSample(getDummyPerIfcCombinedCounters(), LocalDateTime.now())));
        StatisticsBuilder builder = new StatisticsBuilder();
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        verify(statManager).getStatisticsData(anyInt());
        Assert.assertEquals(BigInteger.valueOf(TX_BROADCAST_P), builder.getOutBroadcastPkts().getValue());
        Assert.assertEquals(BigInteger.valueOf(TX_MULTICAST_P), builder.getOutMulticastPkts().getValue());
        Assert.assertEquals(BigInteger.valueOf(TX_UNICAST_P), builder.getOutUnicastPkts().getValue());
        Assert.assertEquals(BigInteger.valueOf(TX_BYTES), builder.getOutOctets().getValue());
        Assert.assertEquals(BigInteger.valueOf(RX_BROADCAST_P), builder.getInBroadcastPkts().getValue());
        Assert.assertEquals(BigInteger.valueOf(RX_MULTICAST_P), builder.getInMulticastPkts().getValue());
        Assert.assertEquals(BigInteger.valueOf(RX_UNICAST_P), builder.getInUnicastPkts().getValue());
        Assert.assertEquals(BigInteger.valueOf(RX_BYTES), builder.getInOctets().getValue());
    }

    @Test
    public void testReadFailed() throws ReadFailedException {
        when(statManager.getStatisticsData(anyInt())).thenReturn(null);
        StatisticsBuilder builder = new StatisticsBuilder();
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
        Assert.assertEquals(BigInteger.ZERO, builder.getInUnicastPkts().getValue());
    }

    private VnetPerInterfaceCombinedCounters getDummyPerIfcCombinedCounters() {
        VnetPerInterfaceCombinedCounters counters = new VnetPerInterfaceCombinedCounters();
        counters.count = 1;
        counters.timestamp = 1;
        VnetCombinedCounter data = new VnetCombinedCounter();
        data.rxBroadcastPackets = RX_BROADCAST_P;
        data.rxMulticastPackets = RX_MULTICAST_P;
        data.rxUnicastPackets = RX_UNICAST_P;
        data.rxBytes = RX_BYTES;
        data.txBroadcastPackets = TX_BROADCAST_P;
        data.txMulticastPackets = TX_MULTICAST_P;
        data.txUnicastPackets = TX_UNICAST_P;
        data.txBytes = TX_BYTES;
        counters.data = new VnetCombinedCounter[]{data};
        return counters;
    }
}
