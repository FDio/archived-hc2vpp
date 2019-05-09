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

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceStatisticsManager;
import io.fd.hc2vpp.v3po.interfacesstate.cache.InterfaceStatisticsManagerImpl;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.VppInterfacesStatsAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces.Statistics;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.v3po.rev190502.interfaces.StatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev180220.Interfaces;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfacesStatisticsCustomizerTest extends WriterCustomizerTest {

    public static final InstanceIdentifier<Statistics> IID =
            InstanceIdentifier.create(Interfaces.class).augmentation(VppInterfacesStatsAugmentation.class)
                    .child(Statistics.class);

    private InterfacesStatisticsCustomizer customizer;
    private InterfaceStatisticsManager statsManager;

    @Override
    protected void setUpTest() throws Exception {
        statsManager = new InterfaceStatisticsManagerImpl();
        customizer = new InterfacesStatisticsCustomizer(statsManager);
    }

    @Test
    public void testWrite() throws WriteFailedException {
        customizer.writeCurrentAttributes(IID, enableStats(true), writeContext);
        Assert.assertTrue(statsManager.isStatisticsEnabled());
    }

    @Test
    public void testUpdatetoEnabled() throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, enableStats(true), enableStats(false), writeContext);
        Assert.assertFalse(statsManager.isStatisticsEnabled());
    }

    @Test
    public void testUpdateToDisabled() throws WriteFailedException {
        customizer.updateCurrentAttributes(IID, enableStats(false), enableStats(true), writeContext);
        Assert.assertTrue(statsManager.isStatisticsEnabled());
    }

    @Test
    public void testDelete() throws WriteFailedException {
        customizer.deleteCurrentAttributes(IID, enableStats(true), writeContext);
        Assert.assertFalse(statsManager.isStatisticsEnabled());
    }

    private Statistics enableStats(final boolean enabled) {
        StatisticsBuilder builder = new StatisticsBuilder();
        return builder.setEnabled(enabled).build();
    }
}
