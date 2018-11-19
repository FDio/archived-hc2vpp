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

package io.fd.hc2vpp.acl.write;

import io.fd.hc2vpp.acl.util.AclContextManager;
import io.fd.hc2vpp.acl.util.FutureJVppAclCustomizer;
import io.fd.hc2vpp.acl.util.acl.AclDataExtractor;
import io.fd.hc2vpp.acl.write.request.AclAddReplaceRequest;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev181001.acls.AclKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AclCustomizer extends FutureJVppAclCustomizer
        implements ListWriterCustomizer<Acl, AclKey>, AclDataExtractor {

    private final AclContextManager standardAclContext;
    private final AclContextManager macIpAclContext;

    public AclCustomizer(@Nonnull final FutureJVppAclFacade jVppAclFacade,
                         @Nonnull final AclContextManager standardAclContext,
                         @Nonnull final AclContextManager macIpAclContext) {
        super(jVppAclFacade);
        this.standardAclContext = standardAclContext;
        this.macIpAclContext = macIpAclContext;
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        AclAddReplaceRequest request = new AclAddReplaceRequest(getjVppAclFacade(), writeContext.getMappingContext());
        if (isStandardAcl(dataAfter)) {
            request.addStandardAcl(id, dataAfter, standardAclContext);
        } else if (isMacIpAcl(dataAfter)) {
            request.addMacIpAcl(id, dataAfter, macIpAclContext);
        } else {
            // double check, first one done by validation
            throw new WriteFailedException.CreateFailedException(id, dataAfter,
                    new IllegalArgumentException("Unsupported acl option"));
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final Acl dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        AclAddReplaceRequest request = new AclAddReplaceRequest(getjVppAclFacade(), writeContext.getMappingContext());

        if (isStandardAcl(dataAfter)) {
            request.updateStandardAcl(id, dataAfter, standardAclContext);
        } else if (isMacIpAcl(dataAfter)) {
            synchronized (macIpAclContext) {
                // there is no direct support for update of mac-ip acl, but only one is allowed per interface
                // so it is atomic from vpp standpoint. Enclosed in synchronized block to prevent issues with
                // multiple threads managing naming context
                request.deleteMacIpAcl(id, dataBefore, macIpAclContext);
                request.addMacIpAcl(id, dataAfter, macIpAclContext);
            }
        } else {
            // double check, first one done by validation
            throw new WriteFailedException.CreateFailedException(id, dataAfter,
                    new IllegalArgumentException("Unsupported acl option"));
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Acl> id, @Nonnull final Acl dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        AclAddReplaceRequest request = new AclAddReplaceRequest(getjVppAclFacade(), writeContext.getMappingContext());

        if (isStandardAcl(dataBefore)) {
            request.deleteStandardAcl(id, dataBefore, standardAclContext);
        } else if (isMacIpAcl(dataBefore)) {
            request.deleteMacIpAcl(id, dataBefore, macIpAclContext);
        } else {
            // double check, first one done by validation
            throw new WriteFailedException.DeleteFailedException(id,
                    new IllegalArgumentException("Unsupported acl option"));
        }
    }
}
