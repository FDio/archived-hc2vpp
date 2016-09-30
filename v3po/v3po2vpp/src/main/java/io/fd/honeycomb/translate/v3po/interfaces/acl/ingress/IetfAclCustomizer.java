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

package io.fd.honeycomb.translate.v3po.interfaces.acl.ingress;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppBaseCallException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.ietf.acl.base.attributes.AccessLists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.ietf.acl.Ingress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Customizer for enabling/disabling ingress ACLs for given interface (as defined in ietf-acl model).
 *
 * The customizer assumes it owns classify table management for interfaces where ietf-acl container is present. Using
 * low level classifier model or direct changes to classify tables in combination with ietf-acls are not supported and
 * can result in unpredictable behaviour.
 */
public class IetfAclCustomizer implements WriterCustomizer<Ingress> {

    private static final Logger LOG = LoggerFactory.getLogger(IetfAclCustomizer.class);
    private final IetfAClWriter aclWriter;
    private final NamingContext interfaceContext;

    public IetfAclCustomizer(@Nonnull final IetfAClWriter aclWriter,
                             @Nonnull final NamingContext interfaceContext) {
        this.aclWriter = checkNotNull(aclWriter, "aclWriter should not be null");
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Ingress> id, @Nonnull final Ingress dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String ifName = id.firstKeyOf(Interface.class).getName();
        final int ifIndex = interfaceContext.getIndex(ifName, writeContext.getMappingContext());
        LOG.debug("Adding ACLs for interface={}(id={}): {}", ifName, ifIndex, dataAfter);

        final AccessLists accessLists = dataAfter.getAccessLists();
        checkArgument(accessLists != null && accessLists.getAcl() != null,
            "ietf-acl container does not define acl list");

        try {
            aclWriter.write(id, ifIndex, accessLists.getAcl(), writeContext);
        } catch (VppBaseCallException e) {
            throw new WriteFailedException.CreateFailedException(id, dataAfter, e);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Ingress> id,
                                        @Nonnull final Ingress dataBefore, @Nonnull final Ingress dataAfter,
                                        @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("ACLs update: removing previously configured ACLs");
        deleteCurrentAttributes(id, dataBefore, writeContext);
        LOG.debug("ACLs update: adding updated ACLs");
        writeCurrentAttributes(id, dataAfter, writeContext);
        LOG.debug("ACLs update was successful");
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Ingress> id,
                                        @Nonnull final Ingress dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final String ifName = id.firstKeyOf(Interface.class).getName();
        final int ifIndex = interfaceContext.getIndex(ifName, writeContext.getMappingContext());
        LOG.debug("Removing ACLs for interface={}(id={}): {}", ifName, ifIndex, dataBefore);
        aclWriter.deleteAcl(id, ifIndex);
    }
}
