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

package io.fd.honeycomb.v3po.impl.trans.w.util;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import io.fd.honeycomb.v3po.impl.trans.util.VppRWUtils;
import io.fd.honeycomb.v3po.impl.trans.w.VppWriter;
import io.fd.honeycomb.v3po.impl.trans.w.WriteContext;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Simple reader registry able to perform and aggregated read (ROOT read) on top of all
 * provided readers. Also able to delegate a specific read to one of the delegate readers.
 *
 * This could serve as a utility to hold & hide all available readers in upper layers.
 */
public final class DelegatingWriterRegistry implements VppWriter<DataObject> {

    private final Map<Class<? extends DataObject>, VppWriter<? extends DataObject>> rootReaders;

    /**
     * Create new {@link DelegatingWriterRegistry}
     *
     * @param rootReaders List of delegate readers
     */
    public DelegatingWriterRegistry(@Nonnull final List<VppWriter<? extends DataObject>> rootReaders) {
        this.rootReaders = VppRWUtils.uniqueLinkedIndex(checkNotNull(rootReaders), VppRWUtils.MANAGER_CLASS_FUNCTION);
    }

    /**
     * @throws UnsupportedOperationException This getter is not supported for reader registry since it does not manage a
     *                                       specific node type
     */
    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        throw new UnsupportedOperationException("Root registry has no type");
    }

    @Override
    public void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                       @Nonnull final List<? extends DataObject> dataBefore,
                       @Nonnull final List<? extends DataObject> dataAfter,
                       @Nonnull final WriteContext ctx) {
        final InstanceIdentifier.PathArgument first = checkNotNull(
            Iterables.getFirst(id.getPathArguments(), null), "Empty id");
        final VppWriter<? extends DataObject> vppWriter = rootReaders.get(first.getType());
        checkNotNull(vppWriter,
            "Unable to write %s. Missing writer. Current writers for: %s", id, rootReaders.keySet());
        vppWriter.update(id, dataBefore, dataAfter, ctx);
    }
}
