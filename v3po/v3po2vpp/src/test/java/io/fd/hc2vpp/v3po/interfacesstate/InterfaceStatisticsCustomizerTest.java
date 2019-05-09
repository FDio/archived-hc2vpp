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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceStatisticsManager;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceStatisticsManagerImpl;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.stats.dto.InterfaceStatistics;
import io.fd.jvpp.stats.dto.InterfaceStatisticsDetails;
import io.fd.jvpp.stats.dto.InterfaceStatisticsDetailsReplyDump;
import io.fd.jvpp.stats.future.FutureJVppStatsFacade;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state._interface.Statistics;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.interfaces.state._interface.StatisticsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceStatisticsCustomizerTest extends ReaderCustomizerTest<Statistics, StatisticsBuilder> {

    private static final String IFC_CTX_NAME = "ifc-test-stats-instance";
    private static final String IF_NAME = "testIfc";
    private static final int SW_IF_INDEX = 1;
    private static final int OUT_ERRORS = 2;
    private static final int OUT_MULTI = 3;
    private static final int OUT_UNI = 4;
    private static final int OUT_BROAD = 5;
    private static final int OUT_BYTES = 6;
    private static final int IN_ERRORS = 22;
    private static final int IN_MULTI = 33;
    private static final int IN_UNI = 44;
    private static final int IN_BROAD = 55;
    private static final int IN_BYTES = 66;
    private NamingContext interfaceContext;
    private static final InstanceIdentifier<Statistics> IID =
            InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
                    .child(Statistics.class);
    @Mock
    private FutureJVppStatsFacade jvppStats;

    private InterfaceStatisticsManager statisticsManager;

    public InterfaceStatisticsCustomizerTest() {
        super(Statistics.class, InterfaceBuilder.class);
    }

    @Override
    public void setUp() {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        interfaceContext.addName(1, IF_NAME, ctx.getMappingContext());
    }

    @Override
    protected ReaderCustomizer<Statistics, StatisticsBuilder> initCustomizer() {
        statisticsManager = new InterfaceStatisticsManagerImpl();
        return new InterfaceStatisticsCustomizer(interfaceContext, jvppStats, statisticsManager);
    }

    @Test
    public void testReadStatistics() throws Exception {
        statisticsManager.enableStatistics();
        when(ctx.getMappingContext().read(any()))
                .thenReturn(Optional.of(new MappingBuilder().setName(IF_NAME).setIndex(SW_IF_INDEX).build()));
        when(jvppStats.interfaceStatisticsDump(any())).thenReturn(future(getStatistics()));
        StatisticsBuilder statBuilder = new StatisticsBuilder();
        getCustomizer().readCurrentAttributes(IID, statBuilder, ctx);
        Statistics stat = statBuilder.build();
        int[] expected = new int[]{SW_IF_INDEX, OUT_ERRORS, OUT_MULTI, OUT_UNI, OUT_BROAD,
                OUT_BYTES, IN_ERRORS, IN_MULTI, IN_UNI, IN_BROAD, IN_BYTES};
        int[] actual = new int[]{interfaceContext.getIndex(IF_NAME, ctx.getMappingContext()),
                stat.getOutErrors().getValue().intValue(), stat.getOutMulticastPkts().getValue().intValue(),
                stat.getOutUnicastPkts().getValue().intValue(), stat.getOutBroadcastPkts().getValue().intValue(),
                stat.getOutOctets().getValue().intValue(), stat.getInErrors().getValue().intValue(),
                stat.getInMulticastPkts().getValue().intValue(), stat.getInUnicastPkts().getValue().intValue(),
                stat.getInBroadcastPkts().getValue().intValue(), stat.getInOctets().getValue().intValue()};

        Assert.assertArrayEquals(expected, actual);
    }

    @Test(expected = ReadFailedException.class)
    public void testReadStatisticsFailed() throws Exception {
        statisticsManager.enableStatistics();
        when(ctx.getMappingContext().read(any()))
                .thenReturn(Optional.of(new MappingBuilder().setName(IF_NAME).setIndex(SW_IF_INDEX).build()));
        when(jvppStats.interfaceStatisticsDump(any())).thenReturn(future(null));
        StatisticsBuilder statBuilder = new StatisticsBuilder();
        getCustomizer().readCurrentAttributes(IID, statBuilder, ctx);
    }

    private InterfaceStatisticsDetailsReplyDump getStatistics() {
        InterfaceStatisticsDetailsReplyDump dumpReply = new InterfaceStatisticsDetailsReplyDump();
        dumpReply.interfaceStatisticsDetails = new InterfaceStatisticsDetails(1, 1);
        dumpReply.interfaceStatisticsDetails.interfaceStatistics[0] =
                new InterfaceStatistics(SW_IF_INDEX, OUT_ERRORS, OUT_MULTI, OUT_UNI, OUT_BROAD,
                        OUT_BYTES, IN_ERRORS, IN_MULTI, IN_UNI, IN_BROAD, IN_BYTES);
        return dumpReply;
    }
}
