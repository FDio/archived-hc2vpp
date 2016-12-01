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

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.RoutingInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Only ensures that requests are written under singleton instance
 */
public class RoutingInstanceCustomizer implements ListWriterCustomizer<RoutingInstance, RoutingInstanceKey> {

    private final RoutingConfiguration configuration;

    public RoutingInstanceCustomizer(@Nonnull final RoutingConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingInstance> id,
                                       @Nonnull final RoutingInstance dataAfter,
                                       @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        checkSingletonInstance(dataAfter);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingInstance> id,
                                        @Nonnull final RoutingInstance dataBefore,
                                        @Nonnull final RoutingInstance dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Operation not supported"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingInstance> id,
                                        @Nonnull final RoutingInstance dataBefore,
                                        @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        checkSingletonInstance(dataBefore);
    }

    private void checkSingletonInstance(final RoutingInstance data) {
        final String defaultRoutingInstanceName = configuration.getDefaultRoutingInstanceName();
        final String instanceName = data.getName();

        checkArgument(defaultRoutingInstanceName.equals(instanceName),
                "Attempt to write/delete data for different than default routing instance detected." +
                        "Vpp allows only single instance, configured with name %s, request contains name %s",
                defaultRoutingInstanceName, instanceName);
    }
}
