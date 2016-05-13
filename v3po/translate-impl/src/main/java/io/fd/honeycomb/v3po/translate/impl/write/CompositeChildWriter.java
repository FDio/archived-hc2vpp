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

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.impl.TraversalType;
import io.fd.honeycomb.v3po.translate.write.ChildWriter;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeChildWriter<D extends DataObject> extends AbstractCompositeWriter<D>
    implements ChildWriter<D> {

    private final ChildWriterCustomizer<D> customizer;

    public CompositeChildWriter(@Nonnull final Class<D> type,
                                @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                                @Nonnull final List<ChildWriter<? extends Augmentation<D>>> augWriters,
                                @Nonnull final ChildWriterCustomizer<D> customizer) {
        this(type, childWriters, augWriters, customizer, TraversalType.PREORDER);
    }


    public CompositeChildWriter(@Nonnull final Class<D> type,
                                @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                                @Nonnull final List<ChildWriter<? extends Augmentation<D>>> augWriters,
                                @Nonnull final ChildWriterCustomizer<D> customizer,
                                @Nonnull final TraversalType traversalType) {
        super(type, childWriters, augWriters, traversalType);
        this.customizer = customizer;
    }

    public CompositeChildWriter(@Nonnull final Class<D> type,
                                @Nonnull final List<ChildWriter<? extends ChildOf<D>>> childWriters,
                                @Nonnull final ChildWriterCustomizer<D> customizer) {
        this(type, childWriters, RWUtils.<D>emptyAugWriterList(), customizer);
    }

    public CompositeChildWriter(@Nonnull final Class<D> type,
                                @Nonnull final ChildWriterCustomizer<D> customizer) {
        this(type, RWUtils.<D>emptyChildWriterList(), RWUtils.<D>emptyAugWriterList(), customizer);
    }

    @Override
    protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D data,
                                          @Nonnull final WriteContext ctx) throws WriteFailedException {
        customizer.writeCurrentAttributes(id, data, ctx);
    }

    @Override
    protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull WriteContext ctx) throws WriteFailedException {
        customizer.deleteCurrentAttributes(id, dataBefore, ctx);
    }

    @Override
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter, @Nonnull WriteContext ctx)
        throws WriteFailedException {
        customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx);
    }

    @Override
    public void writeChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                           @Nonnull final DataObject parentData, @Nonnull WriteContext ctx)
        throws WriteFailedException {
        final InstanceIdentifier<D> currentId = RWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Optional<D> currentData = customizer.extract(currentId, parentData);
        if(currentData.isPresent()) {
            writeCurrent(currentId, currentData.get(), ctx);
        }
    }

    @Override
    public void deleteChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                            @Nonnull final DataObject parentDataBefore,
                            @Nonnull final WriteContext ctx) throws WriteFailedException {
        final InstanceIdentifier<D> currentId = RWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Optional<D> dataBefore = customizer.extract(currentId, parentDataBefore);
        if(dataBefore.isPresent()) {
            deleteCurrent(currentId, dataBefore.get(), ctx);
        }
    }

    @Override
    public void updateChild(@Nonnull final InstanceIdentifier<? extends DataObject> parentId,
                            @Nonnull final DataObject parentDataBefore, @Nonnull final DataObject parentDataAfter,
                            @Nonnull final WriteContext ctx) throws WriteFailedException {
        final InstanceIdentifier<D> currentId = RWUtils.appendTypeToId(parentId, getManagedDataObjectType());
        final Optional<D> before = customizer.extract(currentId, parentDataBefore);
        final Optional<D> after = customizer.extract(currentId, parentDataAfter);

        if(before.isPresent()) {
            if(after.isPresent()) {
                updateCurrent(currentId, before.get(), after.get(), ctx);
            } else {
                deleteCurrent(currentId, before.get(), ctx);
            }
        } else if (after.isPresent()){
            writeCurrent(currentId, after.get(), ctx);
        }
    }
}
