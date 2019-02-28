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


package io.fd.hc2vpp.srv6.write.steering;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.srv6.util.Srv6Util;
import io.fd.hc2vpp.srv6.write.steering.request.L3SteeringRequest;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PrefixCustomizer extends FutureJVppCustomizer implements ListWriterCustomizer<Prefix, PrefixKey> {

    public PrefixCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Prefix> instanceIdentifier,
                                       @Nonnull final Prefix prefix,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        writePrefixes(instanceIdentifier, prefix, writeContext, true).write(instanceIdentifier);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Prefix> instanceIdentifier,
                                        @Nonnull final Prefix prefix,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        writePrefixes(instanceIdentifier, prefix, writeContext, false).delete(instanceIdentifier);
    }

    private L3SteeringRequest writePrefixes(final @Nonnull InstanceIdentifier<Prefix> instanceIdentifier,
                                            final @Nonnull Prefix prefix, final @Nonnull WriteContext writeContext,
                                            final boolean isWrite)
            throws WriteFailedException {
        Ipv6Address bsid = Srv6Util.extractBsid(instanceIdentifier, writeContext, isWrite);
        int vrfFib = Srv6Util.extractVrfFib(instanceIdentifier, writeContext, isWrite);

        if (bsid == null) {
            throw new WriteFailedException.CreateFailedException(instanceIdentifier, prefix,
                    new Throwable("Failed to extract BSID from policy for prefix"));
        }
        // forward only desired traffic to policy
        L3SteeringRequest request = new L3SteeringRequest(getFutureJVpp());
        request.setBindingSid(bsid);
        request.setPrefix(prefix.getIpPrefix());
        request.setFibTableIndex(vrfFib);
        return request;
    }
}
