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

package io.fd.honeycomb.lisp.cfgattrs;

import net.jmob.guice.conf.core.BindConfig;
import net.jmob.guice.conf.core.Syntax;

/**
 * Class containing static configuration for Lisp module,<br>
 * either loaded from property file or statically typed.
 */
@BindConfig(value = "lisp", syntax = Syntax.JSON)
public class LispConfiguration {

    //TODO HONEYCOMB-176 - this constant should be part of V3po plugin
    /**
     * Interface index to name mapping.
     */
    public static final String INTERFACE_CONTEXT = "interface-context";

    /**
     * Locator set index to name mapping.
     */
    public static final String LOCATOR_SET_CONTEXT = "locator-set-context";

    /**
     * Local mappings's eid to name mapping.
     */
    public static final String LOCAL_MAPPING_CONTEXT = "local-mapping-context";

    /**
     * Remote mappings's eid to name mapping.
     */
    public static final String REMOTE_MAPPING_CONTEXT = "remote-mapping-context";

    /**
     * Central honeycomb initializer.
     */
    public static final String HONEYCOMB_INITIALIZER = "honeycomb-initializer";

    /**
     * Unique prefix for naming context of locator sets.
     **/
    public static final String LOCATOR_SET_CONTEXT_PREFIX = "locator-set-";
}
