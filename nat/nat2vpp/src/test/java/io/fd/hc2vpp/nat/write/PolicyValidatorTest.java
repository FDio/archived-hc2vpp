/*
 * Copyright (c) 2018 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.nat.write;

import static io.fd.hc2vpp.nat.NatIds.NAT_INSTANCES_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64PrefixesBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PolicyValidatorTest {

    private static final long VRF_ID = 123;
    private static final InstanceIdentifier<Instance> NAT_INSTANCE_ID =
        NAT_INSTANCES_ID.child(Instance.class, new InstanceKey(VRF_ID));
    private static final InstanceIdentifier<Policy> INVALID_POLICY_ID =
        NAT_INSTANCE_ID.child(Policy.class, new PolicyKey(1L));
    private static final InstanceIdentifier<Policy> DEFAULT_POLICY_ID =
        NAT_INSTANCE_ID.child(Policy.class, new PolicyKey(0L));
    private static final Nat64Prefixes P1 =
        new Nat64PrefixesBuilder().setNat64Prefix(new Ipv6Prefix("2001:db8::1/32")).build();
    private static final Nat64Prefixes P2 =
        new Nat64PrefixesBuilder().setNat64Prefix(new Ipv6Prefix("2001:db8::2/32")).build();

    @Mock
    private WriteContext writeContext;
    private PolicyValidator validator;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        validator = new PolicyValidator();
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testInvalidPolicyId() throws CreateValidationFailedException {
        validator.validateWrite(INVALID_POLICY_ID, mock(Policy.class), writeContext);
    }

    @Test
    public void testNoNat64Prefixes() throws CreateValidationFailedException {
        validator.validateWrite(DEFAULT_POLICY_ID, mock(Policy.class), writeContext);
    }

    @Test
    public void testSingleNat64Prefix() throws CreateValidationFailedException {
        final Policy policy = new PolicyBuilder().setNat64Prefixes(Collections.singletonList(P1)).build();
        validator.validateWrite(DEFAULT_POLICY_ID, policy, writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testTwoNat64Prefixes() throws CreateValidationFailedException {
        final Policy policy = new PolicyBuilder().setNat64Prefixes(Arrays.asList(P1, P2)).build();
        validator.validateWrite(DEFAULT_POLICY_ID, policy, writeContext);
    }
}