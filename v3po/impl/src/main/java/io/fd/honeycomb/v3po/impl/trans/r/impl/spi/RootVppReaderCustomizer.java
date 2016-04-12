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

package io.fd.honeycomb.v3po.impl.trans.r.impl.spi;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * {@link io.fd.honeycomb.v3po.impl.trans.r.impl.CompositeRootVppReader} SPI to customize its behavior
 *
 * @param <C> Specific DataObject derived type, that is handled by this customizer
 * @param <B> Specific Builder for handled type (C)
 */
@Beta
public interface RootVppReaderCustomizer<C extends DataObject, B extends Builder<C>> {

    // TODO add (un)checked, well defined exception here to indicate issues in the customizer

    /**
     * Create new builder that will be used to build read value
     */
    @Nonnull
    B getBuilder(@Nonnull final InstanceIdentifier<C> id);

    /**
     * Add current data (identified by id) to the provided builder
     */
    void readCurrentAttributes(@Nonnull final InstanceIdentifier<C> id, @Nonnull final B builder);
}
