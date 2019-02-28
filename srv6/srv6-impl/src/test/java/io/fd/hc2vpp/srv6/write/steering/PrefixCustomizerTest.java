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

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.translate.util.ByteDataTranslator;
import io.fd.hc2vpp.srv6.Srv6PolicyIIds;
import io.fd.hc2vpp.srv6.util.JvppRequestTest;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.jvpp.core.dto.SrSteeringAddDel;
import io.fd.jvpp.core.dto.SrSteeringAddDelReply;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.autoroute.include.AutorouteInclude;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.include.prefix.ConfigBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.Policies;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.Policy;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.policies.policies.PolicyKey;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.Prefixes;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.Prefix;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixBuilder;
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.oc.srte.policy.rev170918.prefixes.properties.prefixes.PrefixKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PrefixCustomizerTest extends JvppRequestTest {

    private static final PolicyKey POLICY_KEY = new PolicyKey(1L, new IpAddress(new Ipv6Address("e::1")));
    private static final InstanceIdentifier<Prefixes>
            PREFIXES_IID = Srv6PolicyIIds.SR_TE_PLS.child(Policy.class, POLICY_KEY).child(AutorouteInclude.class)
            .child(Prefixes.class);
    private static final Ipv6Address BSID_ADR = new Ipv6Address("a::e");
    private static final IpPrefix IPV6_PREFIX = new IpPrefix(new Ipv6Prefix("a::/64"));
    private static final PrefixKey L3_STEER_KEY = new PrefixKey(IPV6_PREFIX);
    private static final IpPrefix IPV4_PREFIX = new IpPrefix(new Ipv4Prefix("10.0.0.1/24"));
    private static final PrefixKey L3_STEER_KEY_2 = new PrefixKey(IPV4_PREFIX);

    private static final Prefix L3_STEERING_V6 = new PrefixBuilder()
            .setIpPrefix(IPV6_PREFIX)
            .setConfig(new ConfigBuilder().setIpPrefix(IPV6_PREFIX).build())
            .build();

    private static final Prefix L3_STEERING_V4 = new PrefixBuilder()
            .setIpPrefix(IPV4_PREFIX)
            .setConfig(new ConfigBuilder().setIpPrefix(IPV4_PREFIX).build())
            .build();

    private InstanceIdentifier<Prefix> L3_STEER_V6_IID = PREFIXES_IID.child(Prefix.class, L3_STEER_KEY);
    private InstanceIdentifier<Prefix> L3_STEER_V4_IID = PREFIXES_IID.child(Prefix.class, L3_STEER_KEY_2);

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
    public void writeCurrentAttributesV6Test() throws WriteFailedException {
        PrefixCustomizer customizer = new PrefixCustomizer(api);
        customizer.writeCurrentAttributes(L3_STEER_V6_IID, L3_STEERING_V6, ctx);

        verify(api, times(1)).srSteeringAddDel(requestcaptor.capture());
        SrSteeringAddDel srSteering = requestcaptor.getValue();

        testSrSteeringAddDelValidity(srSteering, ByteDataTranslator.BYTE_FALSE, (byte) 6, true, BSID_ADR, IPV6_PREFIX);
    }

    @Test
    public void writeCurrentAttributesV4Test() throws WriteFailedException {
        PrefixCustomizer customizer = new PrefixCustomizer(api);
        customizer.writeCurrentAttributes(L3_STEER_V4_IID, L3_STEERING_V4, ctx);

        verify(api, times(1)).srSteeringAddDel(requestcaptor.capture());
        SrSteeringAddDel srSteering = requestcaptor.getValue();

        testSrSteeringAddDelValidity(srSteering, ByteDataTranslator.BYTE_FALSE, (byte) 4, false, BSID_ADR, IPV4_PREFIX);
    }

    @Test
    public void deleteCurrentAttributesV6Test() throws WriteFailedException {
        PrefixCustomizer customizer = new PrefixCustomizer(api);
        customizer.deleteCurrentAttributes(L3_STEER_V6_IID, L3_STEERING_V6, ctx);

        verify(api, times(1)).srSteeringAddDel(requestcaptor.capture());
        SrSteeringAddDel srSteering = requestcaptor.getValue();

        testSrSteeringAddDelValidity(srSteering, ByteDataTranslator.BYTE_TRUE, (byte) 6, true, BSID_ADR, IPV6_PREFIX);
    }

    @Test
    public void deleteCurrentAttributesV4Test() throws WriteFailedException {
        PrefixCustomizer customizer = new PrefixCustomizer(api);
        customizer.deleteCurrentAttributes(L3_STEER_V4_IID, L3_STEERING_V4, ctx);

        verify(api, times(1)).srSteeringAddDel(requestcaptor.capture());
        SrSteeringAddDel srSteering = requestcaptor.getValue();

        testSrSteeringAddDelValidity(srSteering, ByteDataTranslator.BYTE_TRUE, (byte) 4, false, BSID_ADR, IPV4_PREFIX);
    }
}
