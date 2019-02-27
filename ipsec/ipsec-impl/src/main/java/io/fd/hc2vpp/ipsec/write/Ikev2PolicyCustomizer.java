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
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.JvppReplyConsumer;
import io.fd.hc2vpp.ipsec.FutureJVppIkev2Customizer;
import io.fd.hc2vpp.ipsec.dto.AuthMethod;
import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.ikev2.dto.Ikev2ProfileAddDel;
import io.fd.vpp.jvpp.ikev2.dto.Ikev2ProfileSetAuth;
import io.fd.vpp.jvpp.ikev2.dto.Ikev2ProfileSetTs;
import io.fd.vpp.jvpp.ikev2.future.FutureJVppIkev2Facade;
import java.nio.ByteBuffer;
import javax.annotation.Nonnull;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecIkev2PolicyAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ikev2.policy.aug.grouping.TrafficSelectors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.IkeGeneralPolicyProfileGrouping;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.policy.profile.grouping.Authentication;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ikev2PolicyCustomizer extends FutureJVppIkev2Customizer
        implements ListWriterCustomizer<Policy, PolicyKey>, JvppReplyConsumer, ByteDataTranslator, Ipv4Translator {

    public Ikev2PolicyCustomizer(final FutureJVppIkev2Facade vppApi) {
        super(vppApi);
    }

    @Override
    public void writeCurrentAttributes(@Nonnull final InstanceIdentifier<Policy> id, @Nonnull final Policy dataAfter,
                                       @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final Ikev2ProfileAddDel request = new Ikev2ProfileAddDel();
        request.isAdd = BYTE_TRUE;
        request.name = dataAfter.getName().getBytes();
        getReplyForWrite(getjVppIkev2Facade().ikev2ProfileAddDel(request).toCompletableFuture(), id);
        addAuthorization(dataAfter, id);
        addTrafficSelectors(dataAfter, id);
    }

    @Override
    public void deleteCurrentAttributes(@Nonnull final InstanceIdentifier<Policy> id, @Nonnull final Policy dataBefore,
                                        @Nonnull final WriteContext writeContext) throws WriteFailedException {
        final Ikev2ProfileAddDel request = new Ikev2ProfileAddDel();
        request.isAdd = BYTE_FALSE;
        request.name = dataBefore.getName().getBytes();
        getReplyForWrite(getjVppIkev2Facade().ikev2ProfileAddDel(request).toCompletableFuture(), id);
    }

    @Override
    public void updateCurrentAttributes(@Nonnull final InstanceIdentifier<Policy> id, @Nonnull final Policy dataBefore,
                                        @Nonnull final Policy dataAfter, @Nonnull final WriteContext writeContext)
            throws WriteFailedException {
        addAuthorization(dataAfter, id);
        addTrafficSelectors(dataAfter, id);
    }

    private void addTrafficSelectors(final Policy dataAfter, final InstanceIdentifier<Policy> id)
            throws WriteFailedException {
        IpsecIkev2PolicyAugmentation aug = dataAfter.augmentation(IpsecIkev2PolicyAugmentation.class);
        if (aug == null) {
            return;
        }
        if (aug.getTrafficSelectors() != null) {
            for (TrafficSelectors selector : aug.getTrafficSelectors()) {
                Ikev2ProfileSetTs addTsRequest = new Ikev2ProfileSetTs();
                if (selector.getLocalAddressHigh() != null && selector.getLocalAddressLow() != null) {
                    addTsRequest.isLocal = BYTE_TRUE;
                    addTsRequest.startAddr = ByteBuffer
                            .wrap(ipv4AddressNoZoneToArray(selector.getLocalAddressLow().getIpv4Address().getValue()))
                            .getInt();
                    addTsRequest.endAddr = ByteBuffer
                            .wrap(ipv4AddressNoZoneToArray(selector.getLocalAddressHigh().getIpv4Address().getValue()))
                            .getInt();
                    if (selector.getLocalPortHigh() != null && selector.getLocalPortLow() != null) {
                        addTsRequest.startPort = selector.getLocalPortLow().getValue().shortValue();
                        addTsRequest.endPort = selector.getLocalPortHigh().getValue().shortValue();
                    }
                } else if (selector.getRemoteAddressHigh() != null && selector.getRemoteAddressLow() != null) {
                    addTsRequest.isLocal = BYTE_FALSE;
                    addTsRequest.startAddr = ByteBuffer
                            .wrap(ipv4AddressNoZoneToArray(selector.getRemoteAddressLow().getIpv4Address().getValue()))
                            .getInt();
                    addTsRequest.endAddr = ByteBuffer
                            .wrap(ipv4AddressNoZoneToArray(selector.getRemoteAddressHigh().getIpv4Address().getValue()))
                            .getInt();
                    if (selector.getRemotePortHigh() != null && selector.getRemotePortLow() != null) {
                        addTsRequest.startPort = selector.getRemotePortLow().getValue().shortValue();
                        addTsRequest.endPort = selector.getRemotePortHigh().getValue().shortValue();
                    }
                }
                if (selector.getProtocol() != null) {
                    addTsRequest.proto = selector.getProtocol().byteValue();
                }
                if (dataAfter.getName() != null) {
                    addTsRequest.name = dataAfter.getName().getBytes();
                }
                getReplyForWrite(getjVppIkev2Facade().ikev2ProfileSetTs(addTsRequest).toCompletableFuture(), id);
            }
        }
    }

    private void addAuthorization(final Policy data, final InstanceIdentifier<Policy> id)
            throws WriteFailedException {
        Authentication auth = data.getAuthentication();
        if (auth != null) {
            if (auth.isPresharedKey() != null && data.getPreSharedKey() != null) {
                setProfilePreSharedKeyAuth(data.key().getName(), data.getPreSharedKey(), id);
            } else if (auth.isRsaSignature() != null) {
                IpsecIkev2PolicyAugmentation aug = data.augmentation(IpsecIkev2PolicyAugmentation.class);
                if (aug != null && aug.getCertificate() != null) {
                    setProfileRSAAuth(data.key().getName(), aug.getCertificate(), id);
                }
            }
        }
    }

    private void setProfileRSAAuth(final String name, final String fileName, final InstanceIdentifier<Policy> id)
            throws WriteFailedException {
        Ikev2ProfileSetAuth request = new Ikev2ProfileSetAuth();
        request.name = name.getBytes();
        request.data = fileName.getBytes();
        request.dataLen = request.data.length;
        request.isHex = BYTE_FALSE;
        request.authMethod = AuthMethod.RSA_SIG.getValue();
        getReplyForWrite(getjVppIkev2Facade().ikev2ProfileSetAuth(request).toCompletableFuture(), id);
    }

    private void setProfilePreSharedKeyAuth(final String name,
                                            final IkeGeneralPolicyProfileGrouping.PreSharedKey preSharedKey,
                                            final InstanceIdentifier<Policy> id) throws WriteFailedException {
        final Ikev2ProfileSetAuth request = new Ikev2ProfileSetAuth();
        request.authMethod = AuthMethod.SHARED_KEY_MIC.getValue();
        if (preSharedKey.getHexString() != null) {
            request.isHex = BYTE_TRUE;
        }
        request.data = preSharedKey.stringValue().getBytes();
        request.dataLen = request.data.length;
        request.name = name.getBytes();
        getReplyForWrite(getjVppIkev2Facade().ikev2ProfileSetAuth(request).toCompletableFuture(), id);
    }
}
