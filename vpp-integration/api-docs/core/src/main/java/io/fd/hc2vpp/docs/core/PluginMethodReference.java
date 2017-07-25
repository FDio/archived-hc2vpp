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

package io.fd.hc2vpp.docs.core;

/**
 * Represent found reference of plugin method
 */
public class PluginMethodReference {

    /**
     * Name of the class that uses such reference
     */
    private final String caller;

    /**
     * Class of the reference
     */
    private final String owner;

    /**
     * Name of the reference
     */
    private final String name;

    public PluginMethodReference(final String caller, final String owner, final String name) {
        this.caller = caller;
        this.owner = owner;
        this.name = name;
    }

    public String getCaller() {
        return caller;
    }

    public String getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
            return "PluginMethodReference{name=" + this.name + ", caller=" + this.caller + ", owner=" + this.owner + ", " + "}";
    }
}
