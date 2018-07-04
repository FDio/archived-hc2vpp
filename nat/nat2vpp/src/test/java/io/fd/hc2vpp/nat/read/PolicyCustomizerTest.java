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

package io.fd.hc2vpp.nat.read;

import static io.fd.hc2vpp.nat.read.PolicyCustomizer.DEFAULT_POLICY_ID;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.hc2vpp.nat.NatIds;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.Instances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PolicyCustomizerTest extends ListReaderCustomizerTest<Policy, PolicyKey, PolicyBuilder> {

    private static final InstanceIdentifier<Instance> INSTANCE_ID = InstanceIdentifier.create(Instances.class)
        .child(Instance.class, new InstanceKey(NatInstanceCustomizer.DEFAULT_VRF_ID));

    public PolicyCustomizerTest() {
        super(Policy.class, InstanceBuilder.class);
    }

    @Override
    protected ReaderCustomizer<Policy, PolicyBuilder> initCustomizer() {
        return new PolicyCustomizer();
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {
        assertThat(getCustomizer().getAllIds(NatIds.POLICY_ID, ctx), hasItems(new PolicyKey(0L)));
    }

    @Test
    public void testReadDefault() throws ReadFailedException {
        final PolicyBuilder builder = mock(PolicyBuilder.class);
        getCustomizer().readCurrentAttributes(getId(DEFAULT_POLICY_ID), builder, ctx);
        verify(builder).setId(DEFAULT_POLICY_ID);
    }

    @Test
    public void testReadNonDefault() throws ReadFailedException {
        final PolicyBuilder builder = mock(PolicyBuilder.class);
        getCustomizer().readCurrentAttributes(getId(1L), builder, ctx);
        verifyZeroInteractions(builder);
    }

    private static InstanceIdentifier<Policy> getId(final long id) {
        return INSTANCE_ID.child(Policy.class, new PolicyKey(id));
    }
}