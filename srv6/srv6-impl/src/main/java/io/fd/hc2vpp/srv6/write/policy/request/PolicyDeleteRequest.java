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

package io.fd.hc2vpp.srv6.write.policy.request;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.hc2vpp.srv6.write.DeleteRequest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SrPolicyDel;
import io.fd.jvpp.core.future.FutureJVppCore;
import io.fd.jvpp.core.types.Srv6Sid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PolicyDeleteRequest extends JVppRequest implements DeleteRequest {

    /**
     * Binding SID of the policy
     */
    private Ipv6Address bindingSidAddress;

    public PolicyDeleteRequest(final FutureJVppCore api) {
        super(api);
    }

    @Override
    public void checkValid() {
        checkNotNull(bindingSidAddress, "Binding sid address not set");
    }

    @Override
    public void delete(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        checkValid();
        final SrPolicyDel request = new SrPolicyDel();
        Srv6Sid bsid = new Srv6Sid();
        bsid.addr = ipv6AddressNoZoneToArray(bindingSidAddress);
        request.bsidAddr = bsid;
        getReplyForDelete(getApi().srPolicyDel(request).toCompletableFuture(), identifier);
    }

    public void setBindingSidAddress(
            final Ipv6Address bindingSidAddress) {
        this.bindingSidAddress = bindingSidAddress;
    }
}
