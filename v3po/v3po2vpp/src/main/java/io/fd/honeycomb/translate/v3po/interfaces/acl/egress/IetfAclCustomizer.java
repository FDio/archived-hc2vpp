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

package io.fd.honeycomb.translate.v3po.interfaces.acl.egress;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.acl.common.IetfAclWriter;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev161214.interfaces._interface.ietf.acl.Egress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.InterfaceMode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.acl.rev161214.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IetfAclCustomizer implements WriterCustomizer<Egress> {
    private static final Logger LOG = LoggerFactory.getLogger(IetfAclCustomizer.class);
    private final IetfAclWriter aclWriter;
    private final NamingContext interfaceContext;

    public IetfAclCustomizer(final IetfAclWriter aclWriter, final NamingContext interfaceContext) {
        this.aclWriter = checkNotNull(aclWriter, "aclWriter should not be null");
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String ifName = id.firstKeyOf(Interface.class).getName();
        final int ifIndex = interfaceContext.getIndex(ifName, writeContext.getMappingContext());
        LOG.debug("Adding egress ACLs for interface={}(id={}): {}", ifName, ifIndex, dataAfter);

        final AccessLists accessLists = dataAfter.getAccessLists();
        checkArgument(accessLists != null && accessLists.getAcl() != null,
            "ietf-acl container does not define acl list");

        if (!InterfaceMode.L2.equals(accessLists.getMode())) {
            LOG.debug("Writing egress Acls is supported only in L2 mode. Ignoring config: {}", dataAfter);
            return;
        }

        aclWriter.write(id, ifIndex, accessLists.getAcl(), accessLists.getDefaultAction(), accessLists.getMode(),
            writeContext, writeContext.getMappingContext());
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataBefore,
                                        @Nonnull final Egress dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("ACLs update: removing previously configured ACLs");
        deleteCurrentAttributes(id, dataBefore, writeContext);
        LOG.debug("ACLs update: adding updated ACLs");
        writeCurrentAttributes(id, dataAfter, writeContext);
        LOG.debug("ACLs update was successful");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String ifName = id.firstKeyOf(Interface.class).getName();
        final int ifIndex = interfaceContext.getIndex(ifName, writeContext.getMappingContext());
        LOG.debug("Removing ACLs for interface={}(id={}): {}", ifName, ifIndex, dataBefore);
        aclWriter.deleteAcl(id, ifIndex, writeContext.getMappingContext());
    }
}
