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

package io.fd.honeycomb.v3po.translate.spi.read;

import com.google.common.annotations.Beta;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * CompositeChildReader SPI to customize its behavior.
 *
 * @param <C> Specific DataObject derived type (Identifiable), that is handled by this customizer
 * @param <B> Specific Builder for handled type (C)
 */
@Beta
public interface ReaderCustomizer<C extends DataObject, B extends Builder<C>> {

    /**
     * Creates new builder that will be used to build read value.
     */
    @Nonnull
    B getBuilder(@Nonnull final InstanceIdentifier<C> id);

    /**
     * Adds current data (identified by id) to the provided builder.
     *
     * @param id      id of current data object
     * @param builder builder for creating read value
     * @param ctx
     * @throws ReadFailedException if read was unsuccessful
     */
    void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id,
                               @Nonnull final B builder,
                               @Nonnull final ReadContext ctx) throws ReadFailedException;

    // FIXME need to capture parent builder type, but that's inconvenient at best, is it ok to leave it Builder<?> and
    // cast in specific customizers ? ... probably better than adding another type parameter

    /**
     * Merge read data into provided parent builder.
     */
    void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final C readValue);


}
