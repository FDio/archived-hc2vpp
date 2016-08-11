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

package io.fd.honeycomb.samples.interfaces.mapping.config;

import io.fd.honeycomb.samples.interfaces.mapping.LowerLayerAccess;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.Interface;
import org.opendaylight.yang.gen.v1.io.fd.honeycomb.samples.interfaces.rev160810.interfaces.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a customizer responsible for writing(updating and also deleting) Interface config data
 */
public class InterfaceWriterCustomizer implements ListWriterCustomizer<Interface, InterfaceKey> {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceWriterCustomizer.class);

    private final LowerLayerAccess access;

    public InterfaceWriterCustomizer(final LowerLayerAccess access) {
        this.access = access;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                       @Nonnull final Interface dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        try {
            // Context can be used just like the context in ReadCustomizer see InterfaceReaderCustomizer
            // + it also provides a window into the entire configuration tree before current transaction and during current transaction
            // just in case, some additional data is necessary here
            access.writeInterface(id, dataAfter, writeContext);
        } catch (Exception e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore, @Nonnull final Interface dataAfter,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        // There are cases when lower layer does not support all of the CRUD operations, in which case, the handler
        // should look like this (This will reject configuration from upper layers, returning error/rpc-error):
        throw new WriteFailedException.UpdateFailedException(id, dataBefore, dataAfter,
                new UnsupportedOperationException("Unable to update interface data, unsupported at lower layer"));
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Interface> id,
                                        @Nonnull final Interface dataBefore, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        try {
            // Context can be used just like the context in ReadCustomizer see InterfaceReaderCustomizer
            // + it also provides a window into the entire configuration tree before current transaction and during current transaction
            // just in case, some additional data is necessary here
            access.deleteInterface(id, dataBefore, writeContext);
        } catch (Exception e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }
}
