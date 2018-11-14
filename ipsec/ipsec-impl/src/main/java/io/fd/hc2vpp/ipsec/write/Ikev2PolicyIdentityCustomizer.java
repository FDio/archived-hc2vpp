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

package io.fd.hc2vpp.ipsec.write;

import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.FutureJVppCustomizer;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.Ikev2ProfileSetId;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.identity.grouping.identity.FqdnString;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.identity.grouping.identity.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.identity.grouping.identity.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.identity.grouping.identity.Rfc822AddressString;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ike.general.policy.profile.grouping.Identity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.Policy;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ikev2PolicyIdentityCustomizer extends FutureJVppCustomizer
        implements WriterCustomizer<Identity>, JvppReplyConsumer, ByteDataTranslator, Ipv4Translator, Ipv6Translator {

    public Ikev2PolicyIdentityCustomizer(final FutureJVppCore vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Identity> id,
                                       @Nonnull final Identity dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        String name = id.firstKeyOf(Policy.class).getName();
        if (dataAfter.getLocal() != null) {
            setProfileId(id, name, dataAfter.getLocal().getIdentity(), true);
        }

        if (dataAfter.getRemote() != null) {
            setProfileId(id, name, dataAfter.getRemote().getIdentity(), false);
        }
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Identity> id,
                                        @Nonnull final Identity dataBefore,
                                        @Nonnull final Identity dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        writeCurrentAttributes(id, dataAfter, writeContext);
    }

    private void setProfileId(final InstanceIdentifier<Identity> id,
                              final String profileName,
                              final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.identity.grouping.Identity data,
                              final boolean isLocalId) throws WriteFailedException {
        final Ikev2ProfileSetId request = new Ikev2ProfileSetId();
        request.name = profileName.getBytes();
        transformIdentityToRequest(data, request);
        request.isLocal = isLocalId
                ? BYTE_TRUE
                : BYTE_FALSE;
        getReplyForWrite(getFutureJVpp().ikev2ProfileSetId(request).toCompletableFuture(), id);
    }

    private void transformIdentityToRequest(
            final org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.identity.grouping.Identity
                    identityData, final Ikev2ProfileSetId request) {
        if (identityData instanceof Ipv4Address) {
            request.idType = 1;
            request.data = ipv4AddressNoZoneToArray(((Ipv4Address) identityData).getIpv4Address().getValue());
        } else if (identityData instanceof FqdnString) {
            request.idType = 2;
            request.data = ((FqdnString) identityData).getFqdnString().getValue().getBytes();
        } else if (identityData instanceof Rfc822AddressString) {
            request.idType = 3;
            request.data = ((Rfc822AddressString) identityData).getRfc822AddressString().getBytes();
        } else if (identityData instanceof Ipv6Address) {
            request.idType = 5;
            request.data = ipv6AddressNoZoneToArray(((Ipv6Address) identityData).getIpv6Address());
        }
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Identity> id,
                                        @Nonnull final Identity dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        // VPP doesn't support deletion of Ikev2 Profile ID
    }
}
