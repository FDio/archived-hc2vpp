/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.bgp.inet;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.jvpp.core.dto.IpAddDelRoute;

interface RouteRequestProducer extends ByteDataTranslator {
    int MPLS_LABEL_INVALID = 0x100000;

    default IpAddDelRoute ipAddDelRoute(boolean isAdd) {
        final IpAddDelRoute request = new IpAddDelRoute();
        request.isAdd = booleanToByte(isAdd);
        // we create recursive route and expect hc2vpp user to add route for next hop with interface specified
        request.nextHopSwIfIndex = -1;
        request.nextHopViaLabel = MPLS_LABEL_INVALID;
        return request;
    }
}
