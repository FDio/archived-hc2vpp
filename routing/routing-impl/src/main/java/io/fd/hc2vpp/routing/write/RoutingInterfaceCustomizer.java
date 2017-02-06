/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.routing.write;

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.routing.instance.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy customizer for Interfaces node responsible for validation only.
 */
public class RoutingInterfaceCustomizer implements ListWriterCustomizer<Interface, InterfaceKey> {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingInterfaceCustomizer.class);

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                       @Nonnull final Interface after, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Writing {} after={}", id, after);
        checkIfcIsConfigured(after.getName(), writeContext);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface before, @Nonnull final Interface after,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Updating {} before={} after={}", id, before, after);
        checkIfcIsConfigured(after.getName(), writeContext);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface after, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Removing {}", id);
        // here we do not care if the ifc exists or not, because we are removing reference to it
    }

    private static void checkIfcIsConfigured(@Nonnull final String ifcName, @Nonnull final WriteContext writeContext) {
        final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey>
            id = InstanceIdentifier.create(Interfaces.class)
            .child(
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface.class,
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey(
                    ifcName));
        checkArgument(writeContext.readAfter(id).isPresent(), "Interface %s is not configured");
    }
}
