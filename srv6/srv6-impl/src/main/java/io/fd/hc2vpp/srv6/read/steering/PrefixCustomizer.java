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

package io.fd.hc2vpp.srv6.read.steering;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.srv6.read.steering.request.L3SteeringRequest;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ListReaderCustomizer;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.PrefixesBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixKey;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PrefixCustomizer extends FutureJVppCustomizer
        implements ListReaderCustomizer<Prefix, PrefixKey, PrefixBuilder> {

    public PrefixCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Nonnull
    @Override
    public List<PrefixKey> getAllIds(@Nonnull InstanceIdentifier<Prefix> id, @Nonnull ReadContext context)
            throws ReadFailedException {
        return
                new L3SteeringRequest(getFutureJVpp()).readAllKeys(id, context);
    }

    @Override
    public void merge(@Nonnull Builder<? extends DataObject> builder, @Nonnull List<Prefix> readData) {
        ((PrefixesBuilder) builder).setPrefix(readData);
    }

    @Nonnull
    @Override
    public PrefixBuilder getBuilder(@Nonnull InstanceIdentifier<Prefix> id) {
        return new PrefixBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull InstanceIdentifier<Prefix> id, @Nonnull PrefixBuilder builder,
                                      @Nonnull ReadContext ctx) throws ReadFailedException {
        L3SteeringRequest readRequest = new L3SteeringRequest(getFutureJVpp());
        readRequest.readSpecific(id, ctx, builder);
    }
}
