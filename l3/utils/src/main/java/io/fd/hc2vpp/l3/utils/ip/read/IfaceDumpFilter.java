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

package io.fd.hc2vpp.l3.utils.ip.read;

public class IfaceDumpFilter {

    private final int interfaceIndex;
    private final boolean isIpv6;

    public IfaceDumpFilter(final int interfaceIndex, final boolean isIpv6) {
        this.interfaceIndex = interfaceIndex;
        this.isIpv6 = isIpv6;
    }

    public int getInterfaceIndex() {
        return interfaceIndex;
    }

    public boolean isIpv6() {
        return isIpv6;
    }

    @Override
    public String toString() {
        return "IfaceDumpFilter{" +
                "interfaceIndex=" + interfaceIndex +
                ", isIpv6=" + isIpv6 +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IfaceDumpFilter that = (IfaceDumpFilter) o;

        if (interfaceIndex != that.interfaceIndex) return false;
        return isIpv6 == that.isIpv6;
    }

    @Override
    public int hashCode() {
        int result = interfaceIndex;
        result = 31 * result + (isIpv6 ? 1 : 0);
        return result;
    }
}
