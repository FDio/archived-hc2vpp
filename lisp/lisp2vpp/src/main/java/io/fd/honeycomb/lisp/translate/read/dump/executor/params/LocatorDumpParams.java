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

package io.fd.honeycomb.lisp.translate.read.dump.executor.params;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Params for dumping locators
 */
public final class LocatorDumpParams {

    private final int locatorSetIndex;
    private final byte filter;

    private LocatorDumpParams(LocatorDumpParamsBuilder builder) {
        this.locatorSetIndex = builder.locatorSetIndex;
        this.filter = builder.filter;
    }

    public int getLocatorSetIndex() {
        return locatorSetIndex;
    }

    public byte getFilter() {
        return filter;
    }

    /**
     * Enum for filtering which locators to dump
     */
    public enum LocatorDumpFilter {

        ALL(0),
        LOCAL(1),
        REMOTE(2);

        private final int value;

        private LocatorDumpFilter(int value) {
            this.value = value;
        }

        public final int getValue() {
            return value;
        }
    }

    public static final class LocatorDumpParamsBuilder {

        public int locatorSetIndex;
        public byte filter;


        public LocatorDumpParamsBuilder setLocatorSetIndex(final int locatorSetIndex) {
            this.locatorSetIndex = locatorSetIndex;
            return this;
        }

        public LocatorDumpParamsBuilder setFilter(final LocatorDumpFilter filter) {
            this.filter = Integer.valueOf(checkNotNull(filter, "Cannot set null filter").getValue()).byteValue();
            return this;
        }

        public LocatorDumpParams build() {
            return new LocatorDumpParams(this);
        }
    }
}
