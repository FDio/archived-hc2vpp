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

import io.fd.hc2vpp.srv6.write.DeleteRequest;
import io.fd.hc2vpp.srv6.write.WriteRequest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.SrSteeringAddDel;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Request for steering of L2 traffic
 */
public class L2SteeringRequest extends SteeringRequest implements WriteRequest, DeleteRequest {

    private static final int L2_TRAFFIC_TYPE = 2;

    /**
     * Incoming interface for traffic
     */
    private int inputInterfaceIndex;

    public L2SteeringRequest(final FutureJVppCore api) {
        super(api);
    }


    @Override
    public void delete(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        getReplyForDelete(getApi().srSteeringAddDel(bindRequest(true)).toCompletableFuture(), identifier);
    }

    @Override
    public void write(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        getReplyForWrite(getApi().srSteeringAddDel(bindRequest(false)).toCompletableFuture(), identifier);
    }

    private SrSteeringAddDel bindRequest(final boolean isDel) {
        final SrSteeringAddDel request = new SrSteeringAddDel();
        request.isDel = booleanToByte(isDel);
        request.bsidAddr = ipv6AddressNoZoneToArray(getBindingSid());
        request.swIfIndex = inputInterfaceIndex;
        request.trafficType = L2_TRAFFIC_TYPE;
        return request;
    }

    public void setInputInterfaceIndex(final int inputInterfaceIndex) {
        this.inputInterfaceIndex = inputInterfaceIndex;
    }
}
