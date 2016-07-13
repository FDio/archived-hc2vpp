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

package io.fd.honeycomb.v3po.translate.impl.read;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.v3po.translate.util.read.AbstractGenericReader;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Composite implementation of {@link Reader}.
 */
@Beta
@ThreadSafe
public final class GenericReader<C extends DataObject, B extends Builder<C>> extends AbstractGenericReader<C, B>
    implements Reader<C, B> {

    private final ReaderCustomizer<C, B> customizer;

    /**
     * Create a new {@link GenericReader}.
     *
     * @param id Instance identifier for managed data type
     * @param customizer Customizer instance to customize this generic reader
     */
    public GenericReader(@Nonnull final InstanceIdentifier<C> id,
                         @Nonnull final ReaderCustomizer<C, B> customizer) {
        super(id);
        this.customizer = customizer;
    }

    @Override
    public void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id,
                                      @Nonnull final B builder,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        customizer.readCurrentAttributes(id, builder, ctx);
    }

    @Override
    public B getBuilder(@Nonnull final InstanceIdentifier<C> id) {
        return customizer.getBuilder(id);
    }

    @Override
    public void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final C readValue) {
        customizer.merge(parentBuilder, readValue);
    }

}
