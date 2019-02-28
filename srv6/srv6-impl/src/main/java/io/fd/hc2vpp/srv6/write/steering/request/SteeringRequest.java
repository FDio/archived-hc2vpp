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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;

/**
 * General template for steering requests
 */
public abstract class SteeringRequest extends JVppRequest {

    /**
     * Binding SID of policy that should be applied for this traffic
     */
    private Ipv6Address bindingSid;

    SteeringRequest(final FutureJVppCore api) {
        super(api);
    }

    @Override
    public void checkValid() {
        checkNotNull(bindingSid, "Binding SID is null");
    }

    Ipv6Address getBindingSid() {
        return bindingSid;
    }

    public void setBindingSid(
            final Ipv6Address bindingSid) {
        this.bindingSid = bindingSid;
    }
}
