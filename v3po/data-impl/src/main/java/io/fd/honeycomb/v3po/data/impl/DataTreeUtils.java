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

package io.fd.honeycomb.v3po.data.impl;

import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for various operations on DataTree.
 */
final class DataTreeUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DataTreeUtils.class);

    private DataTreeUtils() {
        throw new UnsupportedOperationException("Can't instantiate util class");
    }

    /**
     * Translates children of supplied YANG ContainerNode into Binding data.
     *
     * @param parent     ContainerNode representing data
     * @param serializer service for serialization between Java Binding Data representation and NormalizedNode
     *                   representation.
     * @return NormalizedNode representation of parent's node children
     */
    static Map<InstanceIdentifier<?>, DataObject> childrenFromNormalized(@Nonnull final DataContainerNode parent,
                                                                         @Nonnull final BindingNormalizedNodeSerializer serializer) {

        Preconditions.checkNotNull(parent, "parent node should not be null");
        Preconditions.checkNotNull(serializer, "serializer should not be null");

        final Map<InstanceIdentifier<?>, DataObject> map = new HashMap<>();

        final Collection<DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?>> children =
                parent.getValue();

        for (final DataContainerChild<? extends YangInstanceIdentifier.PathArgument, ?> child : children) {
            final YangInstanceIdentifier.PathArgument pathArgument = child.getIdentifier();
            final YangInstanceIdentifier identifier = YangInstanceIdentifier.create(pathArgument);
            LOG.debug("DataTreeUtils.childrenFromNormalized() child={}, pathArgument={}, identifier={}", child,
                    pathArgument, identifier);

            final Map.Entry<InstanceIdentifier<?>, DataObject> entry = serializer.fromNormalizedNode(identifier, child);
            if (entry != null) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        return map;
    }
}
