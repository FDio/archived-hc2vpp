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

package io.fd.hc2vpp.v3po.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceCacheStatisticsDumpManager;
import io.fd.vpp.jvpp.core.dto.WantPerInterfaceCombinedStats;
import io.fd.vpp.jvpp.core.dto.WantPerInterfaceCombinedStatsReply;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.VppInterfaceStatsCollectionAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.interfaces._interface.StatisticsCollection;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev181128.interfaces._interface.StatisticsCollectionBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfaceStatisticsEnableCustomizerTest extends WriterCustomizerTest {
    private InterfaceStatisticsEnableCustomizer statCustomizer;
    private static final String IFC_TEST_INSTANCE = "ifc-test-instance";

    private NamingContext namingContext;

    @Mock
    private InterfaceCacheStatisticsDumpManager statManager;

    @Override
    public void setUpTest() throws Exception {
        namingContext = new NamingContext("ifcintest", IFC_TEST_INSTANCE);
        defineMapping(writeContext.getMappingContext(), "tap", 1, IFC_TEST_INSTANCE);
        doReturn(future(new WantPerInterfaceCombinedStatsReply())).when(api).wantPerInterfaceCombinedStats(any());
        statCustomizer = new InterfaceStatisticsEnableCustomizer(this.api, namingContext, statManager);
    }

    @Test
    public void testEnableStatisticsCollection() throws Exception {
        StatisticsCollectionBuilder statCollectionBuilder = new StatisticsCollectionBuilder();
        statCollectionBuilder.setStatisticsEnabled(true);
        statCustomizer.writeCurrentAttributes(getTapId("tap"), statCollectionBuilder.build(), writeContext);
        verify(statManager).enableInterface(anyInt());
        ArgumentCaptor<WantPerInterfaceCombinedStats> argument = ArgumentCaptor.forClass(WantPerInterfaceCombinedStats.class);
        verify(api).wantPerInterfaceCombinedStats(argument.capture());
        Assert.assertEquals(1, argument.getValue().enableDisable);
    }

    @Test
    public void testDisableStatisticsCollection() throws Exception {
        StatisticsCollectionBuilder statCollectionBuilder = new StatisticsCollectionBuilder();
        statCollectionBuilder.setStatisticsEnabled(false);
        statCustomizer.writeCurrentAttributes(getTapId("tap"), statCollectionBuilder.build(), writeContext);
        verify(statManager).disableInterface(anyInt());
        ArgumentCaptor<WantPerInterfaceCombinedStats> argument = ArgumentCaptor.forClass(WantPerInterfaceCombinedStats.class);
        verify(api).wantPerInterfaceCombinedStats(argument.capture());
        Assert.assertEquals(0, argument.getValue().enableDisable);
    }

    @Test
    public void testDeleteStatisticsCollection() throws Exception {
        StatisticsCollectionBuilder statCollectionBuilder = new StatisticsCollectionBuilder();
        statCollectionBuilder.setStatisticsEnabled(true);
        statCustomizer.deleteCurrentAttributes(getTapId("tap"), statCollectionBuilder.build(), writeContext);
        verify(statManager).disableInterface(anyInt());
        ArgumentCaptor<WantPerInterfaceCombinedStats> argument = ArgumentCaptor.forClass(WantPerInterfaceCombinedStats.class);
        verify(api).wantPerInterfaceCombinedStats(argument.capture());
        Assert.assertEquals(0, argument.getValue().enableDisable);
    }

    private InstanceIdentifier<StatisticsCollection> getTapId(final String tap) {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class, new InterfaceKey(tap)).augmentation(
                VppInterfaceStatsCollectionAugmentation.class).child(StatisticsCollection.class);
    }
}
