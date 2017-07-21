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

/**
 * Represents reference to VPP binary api
 */
public class VppApiMessage {

    /**
     * Name of the api
     */
    private final String name;

    /**
     * fd.io doc link
     */
    // TODO - check if possible to add direct link for specific api
    private final String link;

    public VppApiMessage(final String name, final String link) {
        this.name = name;
        this.link = link;
    }

    public String getName() {
        return name;
    }

    public String getLink() {
        return link;
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final VppApiMessage that = (VppApiMessage) o;

        return Objects.equals(this.name, that.name) && Objects.equals(this.link, that.link);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, link);
    }
}
