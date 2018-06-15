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
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PrefixesConfigCustomizer extends FutureJVppCustomizer implements WriterCustomizer<Config> {

    private static final IpPrefix
            DEFAULT_IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("::/0"));
    private static final IpPrefix
            DEFAULT_IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("0.0.0.0/0"));

    public PrefixesConfigCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    private void writePrefixes(final @Nonnull InstanceIdentifier<Config> instanceIdentifier,
                               final @Nonnull Config config, final @Nonnull WriteContext writeContext,
                               final boolean isWrite)
            throws WriteFailedException {
        Ipv6Address bsid = Srv6Util.extractBsid(instanceIdentifier, writeContext, isWrite);
        int vrfFib = Srv6Util.extractVrfFib(instanceIdentifier, writeContext, isWrite);

        if (bsid == null) {
            throw new WriteFailedException.CreateFailedException(instanceIdentifier, config,
                    new Throwable("Failed to extract BSID from policy for prefix"));
        }
        if (config.isPrefixesAll()) {
            // forward all traffic to policy
            writeL3Steering(vrfFib, instanceIdentifier, DEFAULT_IPV6_PREFIX, bsid, getFutureJVpp(), isWrite);
            writeL3Steering(vrfFib, instanceIdentifier, DEFAULT_IPV4_PREFIX, bsid, getFutureJVpp(), isWrite);
        }
    }

    private void writeL3Steering(int vrfFib, InstanceIdentifier instanceIdentifier, IpPrefix ipPrefix,
                                 final Ipv6Address bsid, FutureJVppCore api, boolean isWrite)
            throws WriteFailedException {
        L3SteeringRequest request = new L3SteeringRequest(api);
        request.setBindingSid(bsid);
        request.setPrefix(ipPrefix);
        request.setFibTableIndex(vrfFib);
        if (isWrite) {
            request.write(instanceIdentifier);
        } else {
            request.delete(instanceIdentifier);
        }
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Config> instanceIdentifier,
                                       @Nonnull final Config config,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        writePrefixes(instanceIdentifier, config, writeContext, true);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Config> instanceIdentifier,
                                        @Nonnull final Config config,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        writePrefixes(instanceIdentifier, config, writeContext, false);
    }
}
