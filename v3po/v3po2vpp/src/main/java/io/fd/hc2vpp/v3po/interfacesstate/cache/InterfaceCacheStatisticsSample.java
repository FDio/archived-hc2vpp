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
import java.time.format.DateTimeFormatter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;

public class InterfaceCacheStatisticsSample {
    private final VnetPerInterfaceCombinedCounters data;
    private final DateAndTime captureTime;
    private static final String DATE_AND_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public InterfaceCacheStatisticsSample(final VnetPerInterfaceCombinedCounters data,
                                          final LocalDateTime currentTime) {
        this(data, DateAndTime
                .getDefaultInstance(currentTime.format(DateTimeFormatter.ofPattern(DATE_AND_TIME_PATTERN))));
    }

    public InterfaceCacheStatisticsSample(final VnetPerInterfaceCombinedCounters data,
                                          final DateAndTime currentTime) {
        this.data = data;
        this.captureTime = currentTime;
    }

    public VnetPerInterfaceCombinedCounters getData() {
        return data;
    }

    public DateAndTime getCaptureTime() {
        return captureTime;
    }
}
