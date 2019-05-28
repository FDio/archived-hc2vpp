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

package io.fd.hc2vpp.v3po.read.pbb;

import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.PbbRewriteInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.interfaces._interface.PbbRewriteBuilder;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PbbRewriteCustomizer extends FutureJVppCustomizer
        implements ReaderCustomizer<PbbRewrite, PbbRewriteBuilder> {

    public PbbRewriteCustomizer(@Nonnull final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    @Nonnull
    @Override
    public PbbRewriteBuilder getBuilder(@Nonnull final InstanceIdentifier<PbbRewrite> id) {
        return new PbbRewriteBuilder();
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<PbbRewrite> id,
                                      @Nonnull final PbbRewriteBuilder builder, @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        //TODO implement read after https://jira.fd.io/browse/VPP-468 + init
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder,
                      @Nonnull final PbbRewrite readValue) {
        ((PbbRewriteInterfaceAugmentationBuilder) parentBuilder).setPbbRewrite(readValue);
    }
}
