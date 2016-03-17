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

package io.fd.honeycomb.v3po.impl.trans.impl.spi;

import com.google.common.annotations.Beta;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * io.fd.honeycomb.v3po.impl.trans.impl.CompositeChildVppReader SPI to customize its behavior
 */
@Beta
public interface ChildVppReaderCustomizer<C extends DataObject, B extends Builder<C>> extends
    RootVppReaderCustomizer<C, B> {

    // FIXME need to capture parent builder type, but that's inconvenient at best, is it ok to leave it Builder<?> and
    // cast in specific customizers ? ... probably better than adding another type parameter

    /**
     * Merge read data into provided parent builder
     */
    void merge(@Nonnull final Builder<? extends DataObject> parentBuilder, @Nonnull final C readValue);
}
