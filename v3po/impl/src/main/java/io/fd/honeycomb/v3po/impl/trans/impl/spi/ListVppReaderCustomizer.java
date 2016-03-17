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
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * io.fd.honeycomb.v3po.impl.trans.impl.CompositeListVppReader SPI to customize its behavior
 */
@Beta
public interface ListVppReaderCustomizer<C extends DataObject & Identifiable<K>, K extends Identifier<C>, B extends Builder<C>>
    extends RootVppReaderCustomizer<C, B> {

    /**
     * Return list with IDs of all list nodes to be read.
     *
     * @param id wildcarded ID pointing to list node managed by enclosing reader
     */
    @Nonnull
    List<K> getAllIds(@Nonnull final InstanceIdentifier<C> id);

    /**
     * Merge read data into provided parent builder
     */
    void merge(@Nonnull final Builder<? extends DataObject> builder, @Nonnull final List<C> readData);
}
