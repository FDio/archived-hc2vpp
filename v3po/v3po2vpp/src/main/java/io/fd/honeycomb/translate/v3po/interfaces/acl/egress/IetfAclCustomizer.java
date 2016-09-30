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

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.v3po.interfaces.acl.ingress.IetfAClWriter;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.interfaces._interface.ietf.acl.Egress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IetfAclCustomizer implements WriterCustomizer<Egress> {
    private static final Logger LOG = LoggerFactory.getLogger(IetfAclCustomizer.class);
    private final IetfAClWriter aclWriter;
    private final NamingContext interfaceContext;


    public IetfAclCustomizer(final IetfAClWriter aclWriter, final NamingContext interfaceContext) {
        this.aclWriter = checkNotNull(aclWriter, "aclWriter should not be null");
        this.interfaceContext = checkNotNull(interfaceContext, "interfaceContext should not be null");
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Writing attributes for id={} dataAfter={}: NOT IMPLEMENTED YET", id, dataAfter);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataBefore,
                                        @Nonnull final Egress dataAfter, @Nonnull final WriteContext writeContext)
        throws WriteFailedException {
        LOG.debug("Updating attributes for id={} dataBefore={} dataAfter={}: NOT IMPLEMENTED YET", id, dataBefore, dataAfter);

    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Egress> id, @Nonnull final Egress dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        LOG.debug("Deleting attributes for id={} dataBefore={}: NOT IMPLEMENTED YET", id, dataBefore);
    }
}
