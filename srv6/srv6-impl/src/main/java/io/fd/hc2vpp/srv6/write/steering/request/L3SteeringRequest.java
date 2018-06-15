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

package io.fd.hc2vpp.srv6.write.steering.request;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.srv6.write.DeleteRequest;
import io.fd.hc2vpp.srv6.write.WriteRequest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SrSteeringAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Request for steering of L3 traffic
 */
public class L3SteeringRequest extends SteeringRequest implements WriteRequest, DeleteRequest {

    public static final byte VPP_IPV4_TYPE = 4;
    public static final byte VPP_IPV6_TYPE = 6;
    public static final byte VPP_UNRESOLVED_TYPE = 0;
    /**
     * Where to install FIB entry for this steering
     */
    private int fibTableIndex;

    /**
     * V4/V6 address for traffic type
     */
    private IpPrefix prefix;

    public L3SteeringRequest(final FutureJVppCore api) {
        super(api);
    }

    @Override
    public void checkValid() {
        super.checkValid();
        checkNotNull(prefix, "Prefix is null");
        checkArgument(resolveTrafficType(prefix) == 4 || resolveTrafficType(prefix) == 6,
                "IpPrefix format not recognized");
    }

    @Override
    public void delete(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        checkValid();
        getReplyForDelete(getApi().srSteeringAddDel(bindRequest(true)).toCompletableFuture(), identifier);
    }

    @Override
    public void write(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        checkValid();
        getReplyForWrite(getApi().srSteeringAddDel(bindRequest(false)).toCompletableFuture(), identifier);
    }

    private SrSteeringAddDel bindRequest(final boolean isDel) {
        final SrSteeringAddDel request = new SrSteeringAddDel();
        request.isDel = booleanToByte(isDel);
        request.bsidAddr = ipv6AddressNoZoneToArray(getBindingSid());
        request.tableId = fibTableIndex;
        request.trafficType = resolveTrafficType(prefix);
        request.prefixAddr = ipPrefixToArray(prefix);
        request.maskWidth = extractPrefix(prefix);
        return request;
    }

    public void setFibTableIndex(final int fibTableIndex) {
        this.fibTableIndex = fibTableIndex;
    }

    private byte resolveTrafficType(IpPrefix prefix) {
        if (prefix.getIpv4Prefix() != null) {
            return VPP_IPV4_TYPE;
        } else if (prefix.getIpv6Prefix() != null) {
            return VPP_IPV6_TYPE;
        }
        return VPP_UNRESOLVED_TYPE;
    }

    public void setPrefix(final IpPrefix prefix) {
        this.prefix = prefix;
    }
}
