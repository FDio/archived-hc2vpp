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

package io.fd.honeycomb.v3po.vpp.facade;

import io.fd.honeycomb.v3po.vpp.facade.read.ReadFailedException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.bridge.domain.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ReadFailedExceptionTest {

    @Test
    public void testInstantiation() {
        final InstanceIdentifier<BridgeDomain> id = InstanceIdentifier.create(BridgeDomain.class);
        ReadFailedException e = new ReadFailedException(id);
        Assert.assertEquals(id, e.getFailedId());
        Assert.assertNull(e.getCause());
        Assert.assertTrue(e.getMessage().contains(id.toString()));
    }

    @Test
    public void testInstantiationWithCause() {
        final InstanceIdentifier<Interface> id = InstanceIdentifier.create(Interface.class);
        final RuntimeException cause = new RuntimeException();
        ReadFailedException e = new ReadFailedException(id, cause);
        Assert.assertEquals(id, e.getFailedId());
        Assert.assertEquals(cause, e.getCause());
        Assert.assertTrue(e.getMessage().contains(id.toString()));
    }

    @Test(expected = NullPointerException.class)
    public void testInstantiationFailed() {
        new ReadFailedException(null);
    }
}