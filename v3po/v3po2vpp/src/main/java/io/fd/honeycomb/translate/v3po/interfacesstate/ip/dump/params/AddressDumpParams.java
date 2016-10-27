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

package io.fd.honeycomb.translate.v3po.interfacesstate.ip.dump.params;

public class AddressDumpParams {

    private final int interfaceIndex;
    private final boolean isIpv6;

    public AddressDumpParams(final int interfaceIndex, final boolean isIpv6) {
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
        return "AddressDumpParams{" +
                "interfaceIndex=" + interfaceIndex +
                ", isIpv6=" + isIpv6 +
                '}';
    }
}
