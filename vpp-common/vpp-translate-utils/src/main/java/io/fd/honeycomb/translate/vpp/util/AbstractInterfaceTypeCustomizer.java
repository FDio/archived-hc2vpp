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

package io.fd.honeycomb.translate.vpp.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Optional;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;

/**
 * Validation WriteCustomizers for Interface subnodes.
 * Validates the type of interface.
 */
public abstract class AbstractInterfaceTypeCustomizer<D extends DataObject>
        extends FutureJVppCustomizer implements WriterCustomizer<D> {

    protected AbstractInterfaceTypeCustomizer(final FutureJVppCore futureJVppCore) {
        super(futureJVppCore);
    }

    private void checkProperInterfaceType(@Nonnull final WriteContext writeContext,
                                          @Nonnull final InstanceIdentifier<D> id) {
        final InstanceIdentifier<Interface> ifcTypeFromIid = id.firstIdentifierOf(Interface.class);
        checkArgument(ifcTypeFromIid != null, "Instance identifier does not contain {} type", Interface.class);
        checkArgument(id.firstKeyOf(Interface.class) != null, "Instance identifier does not contain keyed {} type",
                Interface.class);
        final Optional<Interface> interfaceConfig = writeContext.readAfter(ifcTypeFromIid);
        checkState(interfaceConfig.isPresent(),
                "Unable to get Interface configuration for an interface: %s currently being updated", ifcTypeFromIid);

        IllegalInterfaceTypeException
                .checkInterfaceType(interfaceConfig.get(), getExpectedInterfaceType());
    }

    protected abstract Class<? extends InterfaceType> getExpectedInterfaceType();

    /**
     * Validate expected interface type
     */
    @Override
    public final void writeCurrentAttributes(@Nonnull final InstanceIdentifier<D> id, @Nonnull final D dataAfter,
                                             @Nonnull final WriteContext writeContext) throws WriteFailedException {
        checkProperInterfaceType(writeContext, id);
        writeInterface(id, dataAfter, writeContext);
    }

    protected abstract void writeInterface(final InstanceIdentifier<D> id, final D dataAfter,
                                           final WriteContext writeContext)
            throws WriteFailedException;

    // Validation for update and delete is not necessary

    /**
     * Indicates unexpected interface type
     */
    protected static final class IllegalInterfaceTypeException extends IllegalArgumentException {

        private IllegalInterfaceTypeException(final String msg) {
            super(msg);
        }

        /**
         * Check the type of interface equals expected type
         *
         * @throws IllegalInterfaceTypeException if type of interface is null or not expected
         */
        static void checkInterfaceType(@Nonnull final Interface ifc,
                                       @Nonnull final Class<? extends InterfaceType> expectedType) {
            if (ifc.getType() == null || !expectedType.equals(ifc.getType())) {
                throw new IllegalInterfaceTypeException(String.format(
                        "Unexpected interface type: %s for interface: %s. Expected interface is: %s", ifc.getType(),
                        ifc.getName(), expectedType));
            }
        }

    }
}
