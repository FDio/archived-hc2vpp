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

package io.fd.honeycomb.v3po.impl.data;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import io.fd.honeycomb.v3po.impl.trans0.VppReader;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ReadableVppDataTree implementation for operational data.
 */
public final class VppOperationalDataTree implements ReadableVppDataTree {
    private static final Logger LOG = LoggerFactory.getLogger(VppOperationalDataTree.class);
    private final BindingNormalizedNodeSerializer serializer;
    private final VppReader reader;

    /**
     * Creates operational data tree instance.
     *
     * @param serializer service for serialization between Java Binding Data representation and NormalizedNode
     *                   representation.
     * @param reader     service for translation between Vpp and Java Binding Data.
     */
    public VppOperationalDataTree(@Nonnull BindingNormalizedNodeSerializer serializer,
                                  @Nonnull VppReader reader) {
        this.serializer = Preconditions.checkNotNull(serializer, "serializer should not be null");
        this.reader = Preconditions.checkNotNull(reader, "reader should not be null");
    }

    @Override
    public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(
            final YangInstanceIdentifier yangInstanceIdentifier) {
        // TODO What if the path is ROOT/empty?
        final InstanceIdentifier<?> path = serializer.fromYangInstanceIdentifier(yangInstanceIdentifier);
        LOG.debug("VppOperationalDataProxy.read(), path={}", path);

        final DataObject dataObject = reader.read(path); // FIXME we need to expect a list of dataObjects here
        return Futures.immediateCheckedFuture(toNormalizedNode(path, dataObject));
    }

    private Optional<NormalizedNode<?, ?>> toNormalizedNode(final InstanceIdentifier path,
                                                            final DataObject dataObject) {
        LOG.trace("VppOperationalDataProxy.toNormalizedNode(), path={}, path={}", path, dataObject);
        final Map.Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> entry =
                serializer.toNormalizedNode(path, dataObject);

        final NormalizedNode<?, ?> value = entry.getValue();
        LOG.trace("VppOperationalDataProxy.toNormalizedNode(), value={}", value);

        final Optional<NormalizedNode<?, ?>> optional = Optional.<NormalizedNode<?, ?>>fromNullable(value);
        LOG.trace("VppOperationalDataProxy.toNormalizedNode(), optional={}", optional);
        return optional;
    }
}
