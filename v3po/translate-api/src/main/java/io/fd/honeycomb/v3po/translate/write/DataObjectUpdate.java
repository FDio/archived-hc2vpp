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

package io.fd.honeycomb.v3po.translate.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Simple wrapper for BA id + data before and after state. Does not allow both before and after to be null.
 */
public class DataObjectUpdate {

    @Nonnull
    private final InstanceIdentifier<?> id;
    @Nullable
    private final DataObject dataBefore;
    @Nullable
    private final DataObject dataAfter;

    private DataObjectUpdate(@Nonnull final InstanceIdentifier<?> id,
                             @Nullable final DataObject dataBefore,
                             @Nullable final DataObject dataAfter) {
        this.id = checkNotNull(id);
        this.dataAfter = dataAfter;
        this.dataBefore = dataBefore;
    }

    public DataObject getDataBefore() {
        return dataBefore;
    }

    public DataObject getDataAfter() {
        return dataAfter;
    }

    public InstanceIdentifier<?> getId() {
        return id;
    }

    public static DataObjectUpdate create(@Nonnull final InstanceIdentifier<?> id,
                                    @Nullable final DataObject dataBefore,
                                    @Nullable final DataObject dataAfter) {
        checkArgument(!(dataBefore == null && dataAfter == null), "Both before and after data are null");
        if (dataBefore != null) {
            checkArgument(id.getTargetType().isAssignableFrom(dataBefore.getClass()));
        }
        if (dataAfter != null) {
            checkArgument(id.getTargetType().isAssignableFrom(dataAfter.getClass()));
        }

        return dataAfter == null
                ? new DataObjectDelete(id, dataBefore)
                : new DataObjectUpdate(id, dataBefore, dataAfter);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DataObjectUpdate that = (DataObjectUpdate) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "DataObjectUpdate{" + "id=" + id
                + ", dataBefore=" + dataBefore
                + ", dataAfter=" + dataAfter
                + '}';
    }

    public DataObjectUpdate reverse() {
        return DataObjectUpdate.create(id, dataAfter, dataBefore);
    }

    public static class DataObjectDelete extends DataObjectUpdate {

        private DataObjectDelete(@Nonnull final InstanceIdentifier<?> id,
                                 @Nullable final DataObject dataBefore) {
            super(id, dataBefore, null);
        }
    }
}
