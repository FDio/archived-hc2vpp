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

package io.fd.honeycomb.v3po.translate.v3po.interfaces;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import io.fd.honeycomb.v3po.translate.spi.write.ChildWriterCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.FutureJVppCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import io.fd.honeycomb.v3po.translate.v3po.util.WriteTimeoutException;
import io.fd.honeycomb.v3po.translate.write.WriteContext;
import io.fd.honeycomb.v3po.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.Acl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.VppBaseCallException;
import org.openvpp.jvpp.future.FutureJVpp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for enabling/disabling ACLs on given interface
 */
public class AclCustomizer extends FutureJVppCustomizer implements ChildWriterCustomizer<Acl>, AclWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AclCustomizer.class);
    private final NamingContext interfaceContext;
    private final NamingContext classifyTableContext;

    public AclCustomizer(@Nonnull final FutureJVpp vppApi, @Nonnull final NamingContext interfaceContext,
                         @Nonnull final NamingContext classifyTableContext) {
        super(vppApi);
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
        this.classifyTableContext = checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Nonnull
    @Override
    public Optional<Acl> extract(@Nonnull final InstanceIdentifier<Acl> currentId,
                                 @Nonnull final DataObject parentData) {
        return Optional.fromNullable(((VppInterfaceAugmentation) parentData).getAcl());
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
        final String ifName = id.firstKeyOf(Interface.class).getName();
        final int ifIndex = interfaceContext.getIndex(ifName, writeContext.getMappingContext());

        LOG.debug("Setting ACL(isAdd={}) on interface={}(id={}): {}", isAdd, ifName, ifIndex, acl);

        inputAclSetInterface(getFutureJVpp(), isAdd, id, acl, ifIndex, classifyTableContext,
            writeContext.getMappingContext());
        LOG.debug("Successfully set ACL(isAdd={}) on interface={}(id={}): {}", isAdd, ifName, ifIndex, acl);
    }
}
