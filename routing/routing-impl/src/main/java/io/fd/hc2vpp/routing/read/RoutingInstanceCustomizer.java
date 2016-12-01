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

package io.fd.hc2vpp.routing.read;

import com.google.common.collect.ImmutableList;
import io.fd.hc2vpp.routing.RoutingConfiguration;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.RoutingStateBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.StandardRoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.RoutingInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.RoutingInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.routing.rev140524.routing.state.RoutingInstanceKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Returns default instance of routing instance
 */
public class RoutingInstanceCustomizer
        implements ListReaderCustomizer<RoutingInstance, RoutingInstanceKey, RoutingInstanceBuilder> {

    private RoutingInstanceKey defaultKey;

    public RoutingInstanceCustomizer(@Nonnull final RoutingConfiguration configuration) {
        defaultKey = new RoutingInstanceKey(configuration.getDefaultRoutingInstanceName());
    }

    @Nonnull
    @Override
    public List<RoutingInstanceKey> getAllIds(@Nonnull final InstanceIdentifier<RoutingInstance> instanceIdentifier,
                                              @Nonnull final ReadContext readContext) throws ReadFailedException {
        return ImmutableList.of(defaultKey);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<RoutingInstance> list) {
        RoutingStateBuilder.class.cast(builder).setRoutingInstance(list);
    }

    @Nonnull
    @Override
    public RoutingInstanceBuilder getBuilder(@Nonnull final InstanceIdentifier<RoutingInstance> instanceIdentifier) {
        return new RoutingInstanceBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<RoutingInstance> instanceIdentifier,
                                      @Nonnull final RoutingInstanceBuilder routingInstanceBuilder,
                                      @Nonnull final ReadContext readContext) throws ReadFailedException {
        routingInstanceBuilder.setType(StandardRoutingInstance.class)
                .setKey(defaultKey)
                .setName(defaultKey.getName());
    }
}
