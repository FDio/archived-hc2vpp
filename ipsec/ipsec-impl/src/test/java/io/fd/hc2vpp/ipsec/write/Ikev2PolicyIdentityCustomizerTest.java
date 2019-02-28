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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.common.translate.util.Ipv4Translator;
import io.fd.hc2vpp.common.translate.util.Ipv6Translator;
import io.fd.hc2vpp.ipsec.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.ikev2.dto.Ikev2ProfileSetId;
import io.fd.jvpp.ikev2.dto.Ikev2ProfileSetIdReply;
import io.fd.jvpp.ikev2.future.FutureJVppIkev2Facade;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.Ikev2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ike.general.policy.profile.grouping.Identity;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.PolicyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class Ikev2PolicyIdentityCustomizerTest extends WriterCustomizerTest implements SchemaContextTestHelper,
        ByteDataTranslator, Ipv4Translator, Ipv6Translator {

    private static final String POLICY_NAME = "testPolicy";
    private static final String IPV4_TYPE_DATA = "192.168.123.22";
    private static final String IPV6_TYPE_DATA = "2001:DB8:0:0:8:800:200C:417A";
    private static final String FQDN_TYPE_DATA = "vpp.home";
    private static final String RFC822_TYPE_DATA = "rfc822@example.com";
    private static final String IDENTITY_PATH =
            "/hc2vpp-ietf-ipsec:ikev2/hc2vpp-ietf-ipsec:policy[hc2vpp-ietf-ipsec:name='" + POLICY_NAME +
                    "']/hc2vpp-ietf-ipsec:identity";
    private Ikev2PolicyIdentityCustomizer customizer;
    @Mock
    protected FutureJVppIkev2Facade ikev2api;

    @Override
    protected void setUpTest() throws Exception {
        customizer = new Ikev2PolicyIdentityCustomizer(ikev2api);
        when(ikev2api.ikev2ProfileSetId(any())).thenReturn(future(new Ikev2ProfileSetIdReply()));
    }

    @Test
    public void testWriteLocalIpv4(
            @InjectTestData(resourcePath = "/ikev2/identity/identity_local_ipv4.json", id = IDENTITY_PATH) Identity identity)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(getId(), identity, writeContext);
        Ikev2ProfileSetId request = new Ikev2ProfileSetId();
        request.name = POLICY_NAME.getBytes();
        request.idType = (byte) 1;
        request.isLocal = BYTE_TRUE;
        request.data = ipv4AddressNoZoneToArray(IPV4_TYPE_DATA);
        request.dataLen = request.data.length;
        verify(ikev2api).ikev2ProfileSetId(request);
    }

    @Test
    public void testWriteRemoteFqdn(
            @InjectTestData(resourcePath = "/ikev2/identity/identity_remote_fqdn.json", id = IDENTITY_PATH) Identity identity)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(getId(), identity, writeContext);
        Ikev2ProfileSetId request = new Ikev2ProfileSetId();
        request.name = POLICY_NAME.getBytes();
        request.idType = (byte) 2;
        request.isLocal = BYTE_FALSE;
        request.data = FQDN_TYPE_DATA.getBytes();
        request.dataLen = request.data.length;
        verify(ikev2api).ikev2ProfileSetId(request);
    }

    @Test
    public void testWriteLocalIpv6(
            @InjectTestData(resourcePath = "/ikev2/identity/identity_remote_ipv6.json", id = IDENTITY_PATH) Identity identity)
            throws WriteFailedException {
        customizer.writeCurrentAttributes(getId(), identity, writeContext);
        Ikev2ProfileSetId request = new Ikev2ProfileSetId();
        request.name = POLICY_NAME.getBytes();
        request.idType = (byte) 5;
        request.isLocal = BYTE_FALSE;
        request.data = ipv6AddressNoZoneToArray(new Ipv6Address(IPV6_TYPE_DATA));
        request.dataLen = request.data.length;
        verify(ikev2api).ikev2ProfileSetId(request);
    }

    @Test
    public void testUpdate(
            @InjectTestData(resourcePath = "/ikev2/identity/identity_local_ipv4.json", id = IDENTITY_PATH) Identity before,
            @InjectTestData(resourcePath = "/ikev2/identity/identity_local_rfc822.json", id = IDENTITY_PATH) Identity after)
            throws WriteFailedException {
        customizer.updateCurrentAttributes(getId(), before, after, writeContext);
        Ikev2ProfileSetId request = new Ikev2ProfileSetId();
        request.name = POLICY_NAME.getBytes();
        request.idType = (byte) 3;
        request.isLocal = BYTE_TRUE;
        request.data = RFC822_TYPE_DATA.getBytes();
        request.dataLen = request.data.length;
        verify(ikev2api).ikev2ProfileSetId(request);
    }

    private InstanceIdentifier<Identity> getId() {
        return InstanceIdentifier.create(Ikev2.class).child(Policy.class, new PolicyKey(POLICY_NAME))
                .child(Identity.class);
    }
}
