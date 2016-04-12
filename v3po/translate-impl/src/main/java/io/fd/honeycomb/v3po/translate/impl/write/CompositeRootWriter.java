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

package io.fd.honeycomb.v3po.translate.impl.write;

import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.spi.write.RootWriterCustomizer;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeRootWriter<D extends DataObject> extends AbstractCompositeWriter<D> {

    private final RootWriterCustomizer<D> customizer;

    public CompositeRootWriter(@Nonnull final Class<D> type,
                               @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                               @Nonnull final List<ChildWriter<? extends Augmentation<D>>> augWriters,
                               @Nonnull final RootWriterCustomizer<D> customizer) {
        this(type, childWriters, augWriters, customizer, TraversalType.PREORDER);
    }

    public CompositeRootWriter(@Nonnull final Class<D> type,
                               @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                               @Nonnull final List<ChildWriter<? extends Augmentation<D>>> augWriters,
                               @Nonnull final RootWriterCustomizer<D> customizer,
                               @Nonnull final TraversalType traversalType) {
        super(type, childWriters, augWriters, traversalType);
        this.customizer = customizer;
    }

    public CompositeRootWriter(@Nonnull final Class<D> type,
                               @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                               @Nonnull final RootWriterCustomizer<D> customizer) {
        this(type, childWriters, RWUtils.<D>emptyAugWriterList(), customizer);
    }

    public CompositeRootWriter(@Nonnull final Class<D> type,
                               @Nonnull final RootWriterCustomizer<D> customizer) {
        this(type, RWUtils.<D>emptyChildWriterList(), RWUtils.<D>emptyAugWriterList(), customizer);
    }

    @Override
    protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D data,
                                          @Nonnull final WriteContext ctx) throws WriteFailedException {
        // TODO wrap all customizer invocations in try catch, and wrap runtime exceptions in ReadFailed
        // TODO same for readers
        customizer.writeCurrentAttributes(id, data, ctx.getContext());
    }

    @Override
    protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final WriteContext ctx) throws WriteFailedException {
        customizer.deleteCurrentAttributes(id, dataBefore, ctx.getContext());
    }

    @Override
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                           @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter,
                                           @Nonnull final WriteContext ctx) throws WriteFailedException {
        customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx.getContext());
    }
}
