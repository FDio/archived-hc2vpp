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

import io.fd.honeycomb.v3po.translate.spi.write.RootWriterCustomizer;
import io.fd.honeycomb.v3po.translate.Context;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Customizer not performing any changes on current level. Suitable for nodes that don't have any leaves and all of
 * its child nodes are managed by dedicated writers
 */
public class NoopWriterCustomizer<D extends DataObject> implements RootWriterCustomizer<D> {

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataAfter,
                                       @Nonnull final Context ctx) {

    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                        @Nonnull final D dataAfter,
                                        @Nonnull final Context ctx) {

    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataBefore,
                                        @Nonnull final Context ctx) {

    }
}
