/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.routing;

import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.InjectConfig;
import net.jmob.guice.conf.core.Syntax;

@BindConfig(value = "routing", syntax = Syntax.JSON)
public class RoutingConfiguration {

    /**
     * Route ids start from
     */
    public static final int MULTI_MAPPING_START_INDEX = 1;

    /**
     * Contains routing protocol to table id mapping
     */
    public static final String ROUTING_PROTOCOL_CONTEXT = "routing-protocol-context";

    /**
     * Used to map routes to routing-protocols
     */
    public static final String ROUTE_CONTEXT = "route-context";

    /**
     * Used to map hop ids to routes
     */
    public static final String ROUTE_HOP_CONTEXT = "route-hop-context";


    @InjectConfig("default-routing-instance-name")
    private String defaultRoutingInstanceName;

    @InjectConfig("learned-route-name-prefix")
    private String learnedRouteNamePrefix;

    public String getDefaultRoutingInstanceName() {
        return defaultRoutingInstanceName;
    }

    public String getLearnedRouteNamePrefix() {
        return learnedRouteNamePrefix;
    }
}
