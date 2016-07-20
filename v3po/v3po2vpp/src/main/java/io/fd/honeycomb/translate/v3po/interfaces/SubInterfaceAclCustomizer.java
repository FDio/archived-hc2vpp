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

package io.fd.honeycomb.translate.v3po.interfaces;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.v3po.util.SubInterfaceUtils;
import io.fd.honeycomb.translate.v3po.util.WriteTimeoutException;
import io.fd.honeycomb.translate.v3po.vppclassifier.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.Acl;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.core.future.FutureJVppCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for enabling/disabling ACLs on given sub-interface.
 */
public class SubInterfaceAclCustomizer extends FutureJVppCustomizer
    implements WriterCustomizer<Acl>, AclWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SubInterfaceAclCustomizer.class);
    private final NamingContext interfaceContext;
    private final VppClassifierContextManager classifyTableContext;

    public SubInterfaceAclCustomizer(@Nonnull final FutureJVppCore vppApi,
                                     @Nonnull final NamingContext interfaceContext,
                                     @Nonnull final VppClassifierContextManager classifyTableContext) {
        super(vppApi);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        try {
            setAcl(true, id, dataAfter, writeContext);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final Acl dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        throw new UnsupportedOperationException("Acl update is not supported. Please delete Acl container first.");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        try {
            setAcl(false, id, dataBefore, writeContext);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.DeleteFailedException(id, e);
        }
    }

    private void setAcl(final boolean isAdd, @Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl acl,
                        @Nonnull final WriteContext writeContext)
        throws VppBaseCallException, WriteTimeoutException {
        final InterfaceKey parentInterfacekey = id.firstKeyOf(Interface.class);
        final SubInterfaceKey subInterfacekey = id.firstKeyOf(SubInterface.class);
        final String subInterfaceName = SubInterfaceUtils
            .getSubInterfaceName(parentInterfacekey.getName(), subInterfacekey.getIdentifier().intValue());
        final int subInterfaceIndex = interfaceContext.getIndex(subInterfaceName, writeContext.getMappingContext());

        LOG.debug("Setting ACL(isAdd={}) on sub-interface={}(id={}): {}",
            isAdd, subInterfaceName, subInterfaceIndex, acl);
        inputAclSetInterface(getFutureJVpp(), isAdd, id, acl, subInterfaceIndex, classifyTableContext,
            writeContext.getMappingContext());
        LOG.debug("Successfully set ACL(isAdd={}) on sub-interface={}(id={}): {}",
            isAdd, subInterfaceName, subInterfaceIndex, acl);
    }
}
