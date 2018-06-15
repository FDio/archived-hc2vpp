/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.write.sid.request;

import io.fd.hc2vpp.common.translate.util.AddressTranslator;
import io.fd.vpp.jvpp.core.dto.SrLocalsidAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

/**
 * Request for Local SID that should use x-connect functions
 */
public class XConnectLocalSidRequest extends LocalSidFunctionRequest {

    /**
     * L2 interface that should be use for forwarding traffic
     */
    private int outgoingInterfaceIndex;

    /**
     * Outgoing VLan tag
     */
    private int vlanIndex;

    /**
     * Address of the next hop
     */
    private IpAddress nextHopAddress;

    public XConnectLocalSidRequest(final FutureJVppCore api) {
        super(api);
    }

    @Override
    protected void bindRequest(final SrLocalsidAddDel request) {
        super.bindRequest(request);
        request.swIfIndex = getOutgoingInterfaceIndex();
        request.vlanIndex = getVlanIndex();
        if (getNextHopAddress() != null) {
            if (AddressTranslator.INSTANCE.isIpv6(getNextHopAddress())) {
                request.nhAddr6 = ipAddressToArray(getNextHopAddress());
            } else {
                request.nhAddr4 = ipAddressToArray(getNextHopAddress());

            }

        }
    }

    public int getOutgoingInterfaceIndex() {
        return outgoingInterfaceIndex;
    }

    public void setOutgoingInterfaceIndex(final int outgoingInterfaceIndex) {
        this.outgoingInterfaceIndex = outgoingInterfaceIndex;
    }

    public int getVlanIndex() {
        return vlanIndex;
    }

    public void setVlanIndex(final int vlanIndex) {
        this.vlanIndex = vlanIndex;
    }

    public IpAddress getNextHopAddress() {
        return nextHopAddress;
    }

    public void setNextHopAddress(final IpAddress nextHopAddress) {
        this.nextHopAddress = nextHopAddress;
    }
}
