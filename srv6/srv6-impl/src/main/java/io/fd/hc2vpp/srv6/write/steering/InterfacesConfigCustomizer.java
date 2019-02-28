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
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.srv6.util.Srv6Util;
import io.fd.hc2vpp.srv6.write.steering.request.L2SteeringRequest;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.Interface;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.oc.srte.policy.rev180514.sr.interfaces._interface.Config;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class InterfacesConfigCustomizer extends FutureJVppCustomizer implements
        ListWriterCustomizer<Interface, InterfaceKey> {
    private final NamingContext interfaceContext;

    public InterfacesConfigCustomizer(@Nonnull final FutureJVppCore futureJVppCore,
                                      @Nonnull final NamingContext interfaceContext) {
        super(futureJVppCore);
        this.interfaceContext = interfaceContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> instanceIdentifier,
                                       @Nonnull final Interface anInterface, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        if (anInterface.getConfig() != null) {
            writeInterfaces(instanceIdentifier, anInterface.getConfig(), writeContext, true);
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> instanceIdentifier,
                                        @Nonnull final Interface anInterface, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        if (anInterface.getConfig() != null) {
            writeInterfaces(instanceIdentifier, anInterface.getConfig(), writeContext, false);
        }
    }

    private void writeInterfaces(final @Nonnull InstanceIdentifier<Interface> instanceIdentifier,
                                 final @Nonnull Config config, final @Nonnull WriteContext writeContext,
                                 final boolean isWrite)
            throws WriteFailedException {
        Ipv6Address bsid = Srv6Util.extractBsid(instanceIdentifier, writeContext, isWrite);

        if (bsid == null) {
            throw new WriteFailedException.CreateFailedException(instanceIdentifier, config,
                    new Throwable("Failed to extract BSID from policy for prefix"));
        }
        if (config.getInputInterface() != null) {
            // forward all traffic to policy for current interface
            int index = interfaceContext.getIndex(config.getInputInterface(), writeContext.getMappingContext());
            sendL2Steering(instanceIdentifier, bsid, index, getFutureJVpp(), isWrite);
        }
    }

    private void sendL2Steering(final InstanceIdentifier<Interface> instanceIdentifier, final Ipv6Address bsid,
                                final int inputInterface, final FutureJVppCore api, final boolean isWrite)
            throws WriteFailedException {
        L2SteeringRequest request = new L2SteeringRequest(api);
        request.setBindingSid(bsid);
        request.setInputInterfaceIndex(inputInterface);
        if (isWrite) {
            request.write(instanceIdentifier);
        } else {
            request.delete(instanceIdentifier);
        }
    }
}
