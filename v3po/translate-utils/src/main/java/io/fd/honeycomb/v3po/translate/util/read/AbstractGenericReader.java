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

package io.fd.honeycomb.v3po.translate.util.read;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.read.ReadFailedException;
import io.fd.honeycomb.v3po.translate.read.Reader;
import io.fd.honeycomb.v3po.translate.util.RWUtils;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public abstract class AbstractGenericReader<D extends DataObject, B extends Builder<D>> implements Reader<D, B> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractGenericReader.class);

    private final InstanceIdentifier<D> instanceIdentifier;

    protected AbstractGenericReader(final InstanceIdentifier<D> managedDataObjectType) {
        this.instanceIdentifier = RWUtils.makeIidWildcarded(managedDataObjectType);
    }

    @Nonnull
    @Override
    public final InstanceIdentifier<D> getManagedDataObjectType() {
        return instanceIdentifier;
    }

    /**
     * @param id {@link InstanceIdentifier} pointing to current node. In case of keyed list, key must be present.
     *
     */
    protected Optional<D> readCurrent(@Nonnull final InstanceIdentifier<D> id,
                                      @Nonnull final ReadContext ctx) throws ReadFailedException {
        LOG.debug("{}: Reading current: {}", this, id);
        final B builder = getBuilder(id);
        // Cache empty value to determine if anything has changed later TODO cache in a field
        final D emptyValue = builder.build();

        LOG.trace("{}: Reading current attributes", this);
        readCurrentAttributes(id, builder, ctx);

        // Need to check whether anything was filled in to determine if data is present or not.
        final D built = builder.build();
        final Optional<D> read = built.equals(emptyValue)
            ? Optional.absent()
            : Optional.of(built);

        LOG.debug("{}: Current node read successfully. Result: {}", this, read);
        return read;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Optional<? extends DataObject> read(@Nonnull final InstanceIdentifier<? extends DataObject> id,
                                               @Nonnull final ReadContext ctx)
            throws ReadFailedException {
        LOG.trace("{}: Reading : {}", this, id);
        checkArgument(id.getTargetType().equals(getManagedDataObjectType().getTargetType()));
        return readCurrent((InstanceIdentifier<D>) id, ctx);
    }

    @Override
    public String toString() {
        return String.format("Reader[%s]", getManagedDataObjectType().getTargetType().getSimpleName());
    }
}
