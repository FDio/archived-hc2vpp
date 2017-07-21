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

package io.fd.hc2vpp.docs.api;

import java.util.Objects;
import java.util.Set;

/**
 * Represents coverage data for single VPP plugin
 */
public class PluginCoverage {

    /**
     * Name of the covered plugin
     */
    private final String pluginName;

    /**
     * Coverage data
     */
    private final Set<CoverageUnit> coverage;

    /**
     * Whether this is config or operational coverage
     */
    private final boolean isConfig;

    public PluginCoverage(final String pluginName, final Set<CoverageUnit> coverage, final boolean isConfig) {
        this.pluginName = pluginName;
        this.coverage = coverage;
        this.isConfig = isConfig;
    }

    public String getPluginName() {
        return pluginName;
    }

    public Set<CoverageUnit> getCoverage() {
        return coverage;
    }

    public boolean isConfig() {
        return isConfig;
    }

    public boolean hasCoverage() {
        return !coverage.isEmpty();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final PluginCoverage that = (PluginCoverage) o;

        return Objects.equals(this.isConfig, that.isConfig) &&
                Objects.equals(this.pluginName, that.pluginName) &&
                Objects.equals(this.coverage, that.coverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pluginName, this.coverage, this.isConfig);
    }
}

