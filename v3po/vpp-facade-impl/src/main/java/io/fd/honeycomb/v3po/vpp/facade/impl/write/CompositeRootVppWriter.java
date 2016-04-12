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

package io.fd.honeycomb.v3po.vpp.facade.impl.write;

import io.fd.honeycomb.v3po.vpp.facade.impl.util.VppRWUtils;
import io.fd.honeycomb.v3po.vpp.facade.spi.write.RootVppWriterCustomizer;
import io.fd.honeycomb.v3po.vpp.facade.write.ChildVppWriter;
import io.fd.honeycomb.v3po.vpp.facade.write.WriteContext;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CompositeRootVppWriter<D extends DataObject> extends AbstractCompositeVppWriter<D> {

    private final RootVppWriterCustomizer<D> customizer;

    public CompositeRootVppWriter(@Nonnull final Class<D> type,
                                  @Nonnull final List<ChildVppWriter<? extends ChildOf<D>>> childWriters,
                                  @Nonnull final List<ChildVppWriter<? extends Augmentation<D>>> augWriters,
                                  @Nonnull final RootVppWriterCustomizer<D> customizer) {
        super(type, childWriters, augWriters);
        this.customizer = customizer;
    }

    public CompositeRootVppWriter(@Nonnull final Class<D> type,
                                  @Nonnull final List<ChildVppWriter<? extends ChildOf<D>>> childWriters,
                                  @Nonnull final RootVppWriterCustomizer<D> customizer) {
        this(type, childWriters, VppRWUtils.<D>emptyAugWriterList(), customizer);
    }

    public CompositeRootVppWriter(@Nonnull final Class<D> type,
                                  @Nonnull final RootVppWriterCustomizer<D> customizer) {
        this(type, VppRWUtils.<D>emptyChildWriterList(), VppRWUtils.<D>emptyAugWriterList(), customizer);
    }

    @Override
    protected void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D data,
                                          @Nonnull final WriteContext ctx) {
        customizer.writeCurrentAttributes(id, data, ctx.getContext());
    }

    @Override
    protected void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                           @Nonnull final WriteContext ctx) {
        customizer.deleteCurrentAttributes(id, dataBefore, ctx.getContext());
    }

    @Override
    protected void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                           @Nonnull final D dataBefore,
                                           @Nonnull final D dataAfter,
                                           @Nonnull final WriteContext ctx) {
        customizer.updateCurrentAttributes(id, dataBefore, dataAfter, ctx.getContext());
    }
}
