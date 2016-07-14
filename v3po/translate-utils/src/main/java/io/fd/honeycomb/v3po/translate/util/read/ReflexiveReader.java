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

package io.fd.honeycomb.v3po.translate.util.read;

import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Reader that performs no read operation on its own, just fills in the hierarchy.
 * <p/>
 * Might be slow due to reflection !
 */
public class ReflexiveReader<C extends DataObject, B extends Builder<C>> extends AbstractGenericReader<C, B> {

    private final ReflexiveReaderCustomizer<C, B> customizer;

    public ReflexiveReader(final InstanceIdentifier<C> identifier, final Class<B> builderClass) {
        super(identifier);
        this.customizer = new ReflexiveReaderCustomizer<>(identifier.getTargetType(), builderClass);
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id, @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        customizer.readCurrentAttributes(id, builder, ctx);
    }

    @Nonnull
    @Override
    public B getBuilder(final InstanceIdentifier<C> id) {
        return customizer.getBuilder(id);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final C readValue) {
        customizer.merge(parentBuilder, readValue);
    }
}
