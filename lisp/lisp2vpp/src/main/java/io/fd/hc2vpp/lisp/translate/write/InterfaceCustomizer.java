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

package io.fd.hc2vpp.lisp.translate.write;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.nio.charset.StandardCharsets.UTF_8;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import io.fd.vpp.jvpp.core.dto.OneAddDelLocator;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.locator.sets.grouping.locator.sets.locator.set.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


/**
 * Customizer for updating {@link Interface}
 *
 * @see Interface
 */
public class InterfaceCustomizer extends FutureJVppCustomizer
        implements ListWriterCustomizer<Interface, InterfaceKey>, ByteDataTranslator, JvppReplyConsumer {

    private final NamingContext interfaceContext;

    public InterfaceCustomizer(@Nonnull FutureJVppCore futureJvpp, @Nonnull NamingContext interfaceContext) {
        super(futureJvpp);
        this.interfaceContext = checkNotNull(interfaceContext, "Naming context is null");
    }

    @Override
    public void writeCurrentAttributes(InstanceIdentifier<Interface> id, Interface dataAfter, WriteContext writeContext)
            throws WriteFailedException {

        checkNotNull(dataAfter, "Interface is null");
        checkNotNull(dataAfter.getPriority(), "Priority is null");
        checkNotNull(dataAfter.getWeight(), "Weight is null");
        checkState(id.firstKeyOf(Interface.class) != null, "Parent interface not found");
        checkState(id.firstKeyOf(LocatorSet.class) != null, "Parent locator set not found");

        String interfaceName = id.firstKeyOf(Interface.class).getInterfaceRef();
        String locatorSetName = id.firstKeyOf(LocatorSet.class).getName();

        checkState(interfaceContext.containsIndex(interfaceName, writeContext.getMappingContext()),
                "No mapping stored for interface %s", interfaceName);

        try {
            addDelInterfaceAndReply(true, dataAfter,
                    interfaceContext.getIndex(interfaceName, writeContext.getMappingContext()), locatorSetName);
        } catch (VppBaseCallException | TimeoutException | UnsupportedEncodingException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }

    }

    @Override
    public void deleteCurrentAttributes(InstanceIdentifier<Interface> id, Interface dataBefore,
                                        WriteContext writeContext) throws WriteFailedException {
        checkNotNull(dataBefore, "Interface is null");
        checkNotNull(dataBefore.getPriority(), "Priority is null");
        checkNotNull(dataBefore.getWeight(), "Weight is null");
        checkState(id.firstKeyOf(Interface.class) != null, "Parent interface not found");
        checkState(id.firstKeyOf(LocatorSet.class) != null, "Parent locator set not found");

        String interfaceName = id.firstKeyOf(Interface.class).getInterfaceRef();
        String locatorSetName = id.firstKeyOf(LocatorSet.class).getName();

        checkState(interfaceContext.containsIndex(interfaceName, writeContext.getMappingContext()),
                "No mapping stored for interface %s", interfaceName);
        try {
            addDelInterfaceAndReply(false, dataBefore,
                    interfaceContext.getIndex(interfaceName, writeContext.getMappingContext()), locatorSetName);
        } catch (VppBaseCallException | TimeoutException | UnsupportedEncodingException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void addDelInterfaceAndReply(boolean add, Interface data, int interfaceIndex, String locatorSetName)
            throws VppBaseCallException, TimeoutException, UnsupportedEncodingException {
        OneAddDelLocator request = new OneAddDelLocator();

        request.isAdd = booleanToByte(add);
        request.priority = data.getPriority().byteValue();
        request.weight = data.getWeight().byteValue();
        request.swIfIndex = interfaceIndex;
        request.locatorSetName = locatorSetName.getBytes(UTF_8);

        getReply(getFutureJVpp().oneAddDelLocator(request).toCompletableFuture());
    }
}
