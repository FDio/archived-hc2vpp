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

package io.fd.honeycomb.v3po.translate.util.write;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Might be slow !
 */
public class ReflexiveAugmentWriterCustomizer<C extends DataObject> extends NoopWriterCustomizer<C> implements
    ChildWriterCustomizer<C> {

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Optional<C> extract(@Nonnull final InstanceIdentifier<C> currentId, @Nonnull final DataObject parentData) {
        checkArgument(parentData instanceof Augmentable<?>, "Not augmnatable parent object: %s", parentData);
        final Class<C> currentType = currentId.getTargetType();
        final Augmentation<?> augmentation = ((Augmentable) parentData).getAugmentation(currentType);
        if(augmentation == null) {
            return Optional.absent();
        } else {
            checkState(currentType.isAssignableFrom(augmentation.getClass()));
            return Optional.of(currentType.cast(augmentation));
        }
    }
}
