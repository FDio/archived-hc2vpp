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

package io.fd.honeycomb.translate.v3po.interfacesstate.pbb;

import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.FutureJVppCustomizer;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.PbbRewriteStateInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.interfaces.state._interface.PbbRewriteState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.interfaces.state._interface.PbbRewriteStateBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PbbRewriteStateCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<PbbRewriteState, PbbRewriteStateBuilder> {

    public PbbRewriteStateCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Nonnull
    @Override
    public PbbRewriteStateBuilder getBuilder(@Nonnull final InstanceIdentifier<PbbRewriteState> id) {
        return new PbbRewriteStateBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<PbbRewriteState> id,
                                      @Nonnull final PbbRewriteStateBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        //TODO implement read after https://jira.fd.io/browse/VPP-468 + init
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final PbbRewriteState readValue) {
        ((PbbRewriteStateInterfaceAugmentationBuilder) parentBuilder).setPbbRewriteState(readValue);
    }
}
