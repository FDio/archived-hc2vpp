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

package io.fd.hc2vpp.v3po.interfacesstate.cache;

import io.fd.vpp.jvpp.core.dto.VnetPerInterfaceCombinedCounters;
import java.time.LocalDateTime;
import java.util.HashMap;
import javax.annotation.Nullable;

public class InterfaceCacheStatisticsDumpManagerImpl implements InterfaceCacheStatisticsDumpManager {

    private final HashMap<Integer, InterfaceCacheStatisticsSample> statistics;

    public InterfaceCacheStatisticsDumpManagerImpl() {
        statistics = new HashMap<>();
    }

    @Nullable
    @Override
    public InterfaceCacheStatisticsSample getStatisticsData(final int ifcSwIndex) {
        return statistics.getOrDefault(ifcSwIndex, null);
    }

    @Override
    public void setStatisticsData(final VnetPerInterfaceCombinedCounters data, LocalDateTime captureTime,
                                  final int ifcSwIndex) {
        statistics.put(ifcSwIndex, new InterfaceCacheStatisticsSample(data, captureTime));
    }

    @Nullable
    @Override
    public int[] getEnabledInterfaces() {
        return statistics.keySet().stream().mapToInt(i -> i).toArray();
    }

    @Override
    public void disableInterface(final int ifcSwIndex) {
        statistics.remove(ifcSwIndex);
    }

    @Override
    public void disableAll() {
        statistics.clear();
    }

    @Override
    public void enableInterface(final int index) {
        statistics.put(index,
                new InterfaceCacheStatisticsSample(new VnetPerInterfaceCombinedCounters(), LocalDateTime.now()));
    }
}
