/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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

package io.fd.hc2vpp.srv6.write.steering;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.hc2vpp.srv6.util.JvppRequestTest;
import io.fd.hc2vpp.srv6.write.steering.request.L3SteeringRequest;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SrSteeringAddDel;
import io.fd.jvpp.core.dto.SrSteeringAddDelReply;
import java.util.List;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.AutorouteInclude;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.Policies;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.Prefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Config;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PrefixesConfigCustomizerTest extends JvppRequestTest {
    private static final Ipv6Address BSID_ADR = new Ipv6Address("a::e");
    private static final IpPrefix DEFAULT_IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("::/0"));
    private static final IpPrefix DEFAULT_IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("0.0.0.0/0"));
    private static final PolicyKey POLICY_KEY = new PolicyKey(1L, new IpAddress(new Ipv6Address("e::1")));
    private static final Config CONFIG = new ConfigBuilder().setPrefixesAll(true).build();
    private InstanceIdentifier<Config> PREFIXES_CFG_IID =
            Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY).child(AutorouteInclude.class).child(Prefixes.class)
                    .child(Config.class);
    @Captor
    private ArgumentCaptor<SrSteeringAddDel> requestcaptor;

    @InjectTestData(resourcePath = "/policy.json", id = POLICIES_LISTS_PATH)
    private Policies policies;

    @Override
    protected void init() {
        when(api.srSteeringAddDel(any())).thenReturn(future(new SrSteeringAddDelReply()));
        when(ctx.readAfter(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY)))
                .thenReturn(Optional.of(policies.getPolicy().get(0)));
        when(ctx.readBefore(Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY)))
                .thenReturn(Optional.of(policies.getPolicy().get(0)));
    }

    @Test
    public void writeCurrentAttributes() throws WriteFailedException {
        PrefixesConfigCustomizer customizer = new PrefixesConfigCustomizer(api);
        customizer.writeCurrentAttributes(PREFIXES_CFG_IID, CONFIG, ctx);

        verify(api, times(2)).srSteeringAddDel(requestcaptor.capture());
        List<SrSteeringAddDel> srSteerings = requestcaptor.getAllValues();

        testSrSteeringAddDelValidity(srSteerings.get(0), ByteDataTranslator.BYTE_FALSE, L3SteeringRequest.VPP_IPV6_TYPE,
                true, BSID_ADR, DEFAULT_IPV6_PREFIX);
        testSrSteeringAddDelValidity(srSteerings.get(1), ByteDataTranslator.BYTE_FALSE,
                L3SteeringRequest.VPP_IPV4_TYPE, false, BSID_ADR, DEFAULT_IPV4_PREFIX);
    }

    @Test
    public void deleteCurrentAttributes() throws WriteFailedException {
        PrefixesConfigCustomizer customizer = new PrefixesConfigCustomizer(api);
        customizer.deleteCurrentAttributes(PREFIXES_CFG_IID, CONFIG, ctx);

        verify(api, times(2)).srSteeringAddDel(requestcaptor.capture());
        List<SrSteeringAddDel> srSteerings = requestcaptor.getAllValues();

        testSrSteeringAddDelValidity(srSteerings.get(0), ByteDataTranslator.BYTE_TRUE, L3SteeringRequest.VPP_IPV6_TYPE,
                true, BSID_ADR, DEFAULT_IPV6_PREFIX);
        testSrSteeringAddDelValidity(srSteerings.get(1), ByteDataTranslator.BYTE_TRUE, L3SteeringRequest.VPP_IPV4_TYPE,
                false, BSID_ADR, DEFAULT_IPV4_PREFIX);
    }

}
