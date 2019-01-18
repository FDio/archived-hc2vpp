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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.ipsec.dto.AuthMethod;
import io.fd.hc2vpp.ipsec.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.Ikev2ProfileAddDel;
import io.fd.vpp.jvpp.core.dto.Ikev2ProfileAddDelReply;
import io.fd.vpp.jvpp.core.dto.Ikev2ProfileSetAuth;
import io.fd.vpp.jvpp.core.dto.Ikev2ProfileSetAuthReply;
import io.fd.vpp.jvpp.core.dto.Ikev2ProfileSetTs;
import io.fd.vpp.jvpp.core.dto.Ikev2ProfileSetTsReply;
import java.nio.ByteBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecIkev2PolicyAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.ikev2.policy.aug.grouping.TrafficSelectors;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.Ikev2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.policy.profile.grouping.Authentication;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class Ikev2PolicyCustomizerTest extends WriterCustomizerTest implements SchemaContextTestHelper,
        ByteDataTranslator, Ipv4Translator, Ipv6Translator {

    private static final String IKEV2_PATH = "/hc2vpp-ietf-ipsec:ikev2";
    private Ikev2PolicyCustomizer customizer;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new Ikev2PolicyCustomizer(api);
        when(api.ikev2ProfileAddDel(any())).thenReturn(future(new Ikev2ProfileAddDelReply()));
        when(api.ikev2ProfileSetTs(any())).thenReturn(future(new Ikev2ProfileSetTsReply()));
        when(api.ikev2ProfileSetAuth(any())).thenReturn(future(new Ikev2ProfileSetAuthReply()));
    }

    @Test
    public void testWrite(@InjectTestData(resourcePath = "/ikev2/addIkev2Profile.json", id = IKEV2_PATH) Ikev2 ikev2)
            throws WriteFailedException {
        Policy policy = ikev2.getPolicy().get(0);
        customizer.writeCurrentAttributes(getId(policy.getName()), policy, writeContext);
        Ikev2ProfileAddDel profileAddrequest = new Ikev2ProfileAddDel();
        profileAddrequest.isAdd = BYTE_TRUE;
        profileAddrequest.name = policy.getName().getBytes();

        verify(api).ikev2ProfileAddDel(profileAddrequest);
        verify(api).ikev2ProfileSetTs(translateTStoRequest(policy.augmentation(IpsecIkev2PolicyAugmentation.class)
                .getTrafficSelectors().get(0), policy.getName()));
        verify(api).ikev2ProfileSetAuth(translateAuthToRequest(policy));
    }

    @Test
    public void testUpdate(
            @InjectTestData(resourcePath = "/ikev2/addDelProfile_before.json", id = IKEV2_PATH) Ikev2 ikev2Before,
            @InjectTestData(resourcePath = "/ikev2/addDelProfile_after.json", id = IKEV2_PATH) Ikev2 ikev2After)
            throws WriteFailedException {
        final Policy before = ikev2Before.getPolicy().get(0);
        final Policy after = ikev2After.getPolicy().get(0);
        customizer.updateCurrentAttributes(getId(before.getName()), before, after, writeContext);

        verify(api, times(0)).ikev2ProfileAddDel(any());
        verify(api).ikev2ProfileSetTs(translateTStoRequest(after.augmentation(IpsecIkev2PolicyAugmentation.class)
                .getTrafficSelectors().get(0), after.getName()));
        verify(api).ikev2ProfileSetAuth(translateAuthToRequest(after));
    }

    @Test
    public void testDelete(@InjectTestData(resourcePath = "/ikev2/addIkev2Profile.json", id = IKEV2_PATH) Ikev2 ikev2)
            throws WriteFailedException {
        Policy policy = ikev2.getPolicy().get(0);
        customizer.deleteCurrentAttributes(getId(policy.getName()), policy, writeContext);
        final Ikev2ProfileAddDel request = new Ikev2ProfileAddDel();
        request.name = policy.getName().getBytes();
        request.isAdd = BYTE_FALSE;
        verify(api).ikev2ProfileAddDel(request);
        verify(api, times(0)).ikev2ProfileSetTs(any());
        verify(api, times(0)).ikev2ProfileSetAuth(any());
    }

    private InstanceIdentifier<Policy> getId(final String name) {
        return InstanceIdentifier.create(Ikev2.class).child(Policy.class, new PolicyKey(name));
    }

    private Ikev2ProfileSetTs translateTStoRequest(final TrafficSelectors selector, final String policyName) {
        Ikev2ProfileSetTs addTsRequest = new Ikev2ProfileSetTs();
        if (selector.getLocalAddressHigh() != null && selector.getLocalAddressLow() != null) {
            addTsRequest.isLocal = ByteDataTranslator.BYTE_TRUE;
            addTsRequest.startAddr = ByteBuffer
                    .wrap(ipv4AddressNoZoneToArray(selector.getLocalAddressLow().getIpv4Address().getValue())).getInt();
            addTsRequest.endAddr = ByteBuffer
                    .wrap(ipv4AddressNoZoneToArray(selector.getLocalAddressHigh().getIpv4Address().getValue()))
                    .getInt();
            if (selector.getLocalPortHigh() != null && selector.getLocalPortLow() != null) {
                addTsRequest.startPort = selector.getLocalPortLow().getValue().shortValue();
                addTsRequest.endPort = selector.getLocalPortHigh().getValue().shortValue();
            }
        } else if (selector.getRemoteAddressHigh() != null && selector.getRemoteAddressLow() != null) {
            addTsRequest.isLocal = ByteDataTranslator.BYTE_FALSE;
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
        if (policyName != null) {
            addTsRequest.name = policyName.getBytes();
        }
        return addTsRequest;
    }

    private Ikev2ProfileSetAuth translateAuthToRequest(Policy policy) {
        final Ikev2ProfileSetAuth request = new Ikev2ProfileSetAuth();
        Authentication auth = policy.getAuthentication();
        if (auth != null) {
            request.name = policy.getName().getBytes();
            if (auth.isPresharedKey() != null && policy.getPreSharedKey() != null) {
                request.authMethod = AuthMethod.SHARED_KEY_MIC.getValue();
                if (policy.getPreSharedKey().getHexString() != null) {
                    request.isHex = ByteDataTranslator.BYTE_TRUE;
                }
                request.data = policy.getPreSharedKey().stringValue().getBytes();
                request.dataLen = request.data.length;
            } else if (auth.isRsaSignature() != null) {
                IpsecIkev2PolicyAugmentation aug = policy.augmentation(IpsecIkev2PolicyAugmentation.class);
                if (aug != null && aug.getCertificate() != null) {
                    request.data = aug.getCertificate().getBytes();
                    request.dataLen = request.data.length;
                    request.authMethod = AuthMethod.RSA_SIG.getValue();
                }
            }
        }
        return request;
    }
}
