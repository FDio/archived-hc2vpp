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

import static com.google.common.base.Preconditions.checkArgument;

import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import io.fd.honeycomb.v3po.translate.write.Writer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractCompositeWriter<D extends DataObject> implements Writer<D> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCompositeWriter.class);

    private final InstanceIdentifier<D> instanceIdentifier;

    AbstractCompositeWriter(final InstanceIdentifier<D> type) {
        this.instanceIdentifier = type;
    }

    protected void writeCurrent(final InstanceIdentifier<D> id, final D data, final WriteContext ctx)
        throws WriteFailedException {
        LOG.debug("{}: Writing current: {} data: {}", this, id, data);
        writeCurrentAttributes(id, data, ctx);
        LOG.debug("{}: Current node written successfully", this);
    }

    protected void updateCurrent(final InstanceIdentifier<D> id, final D dataBefore, final D dataAfter,
                                 final WriteContext ctx) throws WriteFailedException {
        LOG.debug("{}: Updating current: {} dataBefore: {}, datAfter: {}", this, id, dataBefore, dataAfter);

        if (dataBefore.equals(dataAfter)) {
            LOG.debug("{}: Skipping current(no update): {}", this, id);
            // No change, ignore
            return;
        }
        updateCurrentAttributes(id, dataBefore, dataAfter, ctx);
        LOG.debug("{}: Current node updated successfully", this);
    }

    protected void deleteCurrent(final InstanceIdentifier<D> id, final D dataBefore, final WriteContext ctx)
        throws WriteFailedException {
        LOG.debug("{}: Deleting current: {} dataBefore: {}", this, id, dataBefore);
        deleteCurrentAttributes(id, dataBefore, ctx);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                       @Nullable final DataObject dataBefore,
                       @Nullable final DataObject dataAfter,
                       @Nonnull final WriteContext ctx) throws WriteFailedException {
        LOG.debug("{}: Updating : {}", this, id);
        LOG.trace("{}: Updating : {}, from: {} to: {}", this, id, dataBefore, dataAfter);

        checkArgument(idPointsToCurrent(id), "Cannot handle data: %s. Only: %s can be handled by writer: %s",
                id, getManagedDataObjectType(), this);

        if (isWrite(dataBefore, dataAfter)) {
            writeCurrent((InstanceIdentifier<D>) id, castToManaged(dataAfter), ctx);
        } else if (isDelete(dataBefore, dataAfter)) {
            deleteCurrent((InstanceIdentifier<D>) id, castToManaged(dataBefore), ctx);
        } else {
            checkArgument(dataBefore != null && dataAfter != null, "No data to process");
            updateCurrent((InstanceIdentifier<D>) id, castToManaged(dataBefore), castToManaged(dataAfter), ctx);
        }
    }

    private void checkDataType(@Nonnull final DataObject dataAfter) {
        checkArgument(getManagedDataObjectType().getTargetType().isAssignableFrom(dataAfter.getClass()));
    }

    private D castToManaged(final DataObject data) {
        checkDataType(data);
        return getManagedDataObjectType().getTargetType().cast(data);
    }

    private static boolean isWrite(final DataObject dataBefore,
                                    final DataObject dataAfter) {
        return dataBefore == null && dataAfter != null;
    }

    private static boolean isDelete(final DataObject dataBefore,
                                    final DataObject dataAfter) {
        return dataAfter == null && dataBefore != null;
    }

    private boolean idPointsToCurrent(final @Nonnull InstanceIdentifier<? extends DataObject> id) {
        return id.getTargetType().equals(getManagedDataObjectType().getTargetType());
    }

    protected abstract void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                   @Nonnull final D data,
                                                   @Nonnull final WriteContext ctx) throws WriteFailedException;

    protected abstract void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                    @Nonnull final D dataBefore,
                                                    @Nonnull final WriteContext ctx) throws WriteFailedException;

    protected abstract void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id,
                                                    @Nonnull final D dataBefore,
                                                    @Nonnull final D dataAfter,
                                                    @Nonnull final WriteContext ctx) throws WriteFailedException;

    @Nonnull
    @Override
    public InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }


    @Override
    public String toString() {
        return String.format("Writer[%s]", getManagedDataObjectType().getTargetType().getSimpleName());
    }
}
