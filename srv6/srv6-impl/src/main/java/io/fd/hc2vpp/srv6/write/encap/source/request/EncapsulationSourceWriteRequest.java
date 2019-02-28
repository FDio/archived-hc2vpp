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

package io.fd.hc2vpp.srv6.write.encap.source.request;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.srv6.util.JVppRequest;
import io.fd.hc2vpp.srv6.write.WriteRequest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SrSetEncapSource;
import io.fd.jvpp.core.future.FutureJVppCore;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class EncapsulationSourceWriteRequest extends JVppRequest implements WriteRequest {

    private Ipv6Address bsid;

    public EncapsulationSourceWriteRequest(final FutureJVppCore api) {
        super(api);
    }

    @Override
    public void checkValid() {
        checkNotNull(bsid, "Binding SID must be set");
    }

    public Ipv6Address getBsid() {
        return bsid;
    }

    public EncapsulationSourceWriteRequest setBsid(final Ipv6Address bsid) {
        this.bsid = bsid;
        return this;
    }

    @Override
    public void write(final InstanceIdentifier<?> identifier) throws WriteFailedException {
        checkValid();
        final SrSetEncapSource request = new SrSetEncapSource();
        request.encapsSource = ipv6AddressNoZoneToArray(bsid);
        getReplyForWrite(getApi().srSetEncapSource(request).toCompletableFuture(), identifier);
    }
}
