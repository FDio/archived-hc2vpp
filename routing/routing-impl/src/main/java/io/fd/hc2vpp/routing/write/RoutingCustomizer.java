/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
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

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.Routing;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy customizer for Root node,to "handle" non-operational changes
 */
public class RoutingCustomizer implements WriterCustomizer<Routing> {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingCustomizer.class);

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                       @Nonnull final Routing routing, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Writing {}", instanceIdentifier);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Routing> instanceIdentifier,
                                        @Nonnull final Routing routing, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        LOG.debug("Removing {}", instanceIdentifier);
    }
}
