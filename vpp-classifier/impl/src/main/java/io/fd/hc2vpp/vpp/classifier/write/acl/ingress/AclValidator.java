/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.vpp.classifier.write.acl.ingress;

import static com.google.common.base.Preconditions.checkNotNull;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.honeycomb.translate.write.DataValidationFailedException;
import io.fd.honeycomb.translate.write.Validator;
import io.fd.honeycomb.translate.write.WriteContext;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.AclBaseAttributes;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.Ingress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AclValidator implements Validator<Ingress> {

    public AclValidator(@Nonnull final NamingContext interfaceContext,
                        @Nonnull final VppClassifierContextManager classifyTableContext) {
        checkNotNull(interfaceContext, "interfaceContext should not be null");
        checkNotNull(classifyTableContext, "classifyTableContext should not be null");
    }

    @Override
    public void validateWrite(@Nonnull final InstanceIdentifier<Ingress> id,
                              @Nonnull final Ingress acl,
                              @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.CreateValidationFailedException {
        try {
            validateAcl(acl);
        } catch (RuntimeException e) {
            throw new DataValidationFailedException.CreateValidationFailedException(id, acl, e);
        }
    }

    @Override
    public void validateDelete(@Nonnull final InstanceIdentifier<Ingress> id,
                               @Nonnull final Ingress dataBefore,
                               @Nonnull final WriteContext writeContext)
            throws DataValidationFailedException.DeleteValidationFailedException {
        try {
            validateAcl(dataBefore);
        } catch (RuntimeException e) {
            throw new DataValidationFailedException.DeleteValidationFailedException(id, e);
        }
    }

    private void validateAcl(@Nonnull final AclBaseAttributes acl) {
        final L2Acl l2Acl = acl.getL2Acl();
        if (l2Acl != null) {
            checkNotNull(l2Acl.getClassifyTable(), "L2 classify table is null");
        }
        final Ip4Acl ip4Acl = acl.getIp4Acl();
        if (ip4Acl != null) {
            checkNotNull(ip4Acl.getClassifyTable(), "IPv4 classify table is null");
        }
        final Ip6Acl ip6Acl = acl.getIp6Acl();
        if (ip6Acl != null) {
            checkNotNull(ip6Acl.getClassifyTable(), "IPv6 classify table is null");
        }
    }
}
