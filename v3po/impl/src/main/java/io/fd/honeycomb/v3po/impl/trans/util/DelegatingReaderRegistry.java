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

package io.fd.honeycomb.v3po.impl.trans.util;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.fd.honeycomb.v3po.impl.trans.ReaderRegistry;
import io.fd.honeycomb.v3po.impl.trans.VppReader;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class DelegatingReaderRegistry implements ReaderRegistry {

    private final Map<Class<? extends DataObject>, VppReader<? extends DataObject>> rootReaders;

    public DelegatingReaderRegistry(@Nonnull final List<VppReader<? extends DataObject>> rootReaders) {
        this.rootReaders = toMap(rootReaders);
    }

    private static Map<Class<? extends DataObject>, VppReader<? extends DataObject>> toMap(
        final List<VppReader<? extends DataObject>> rootReaders) {
        return Maps
            .uniqueIndex(rootReaders, new Function<VppReader<? extends DataObject>, Class<? extends DataObject>>() {
                @Override
                public Class<? extends DataObject> apply(final VppReader<? extends DataObject> input) {
                    return input.getManagedDataObjectType().getTargetType();
                }
            });
    }

    @Override
    @Nonnull
    public List<? extends DataObject> readAll() {
        final List<DataObject> objects = Lists.newArrayListWithExpectedSize(rootReaders.size());
        for (VppReader<? extends DataObject> rootReader : rootReaders.values()) {
            final List<? extends DataObject> read = rootReader.read(rootReader.getManagedDataObjectType());
            objects.addAll(read);
        }
        return objects;
    }

    @Nonnull
    @Override
    public List<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id) {
        final InstanceIdentifier.PathArgument first = Preconditions.checkNotNull(
            Iterables.getFirst(id.getPathArguments(), null), "Empty id");
        final VppReader<? extends DataObject> vppReader = rootReaders.get(first.getType());
        Preconditions.checkNotNull(vppReader,
            "Unable to read %s. Missing reader. Current readers for: %s", id, rootReaders.keySet());
        return vppReader.read(id);
    }

    @Nonnull
    @Override
    public InstanceIdentifier<DataObject> getManagedDataObjectType() {
        throw new UnsupportedOperationException("Root registry has no type");
    }

}
