/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.jvpp.nat.dto.Nat64PrefixDetails;
import io.fd.jvpp.nat.dto.Nat64PrefixDetailsReplyDump;
import io.fd.jvpp.nat.future.FutureJVppNatFacade;
import io.fd.jvpp.nat.types.Ip6Address;
import io.fd.jvpp.nat.types.Ip6Prefix;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.Instances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.Policy;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.PolicyKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64Prefixes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64PrefixesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.instance.policy.Nat64PrefixesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Nat64PrefixesCustomizerTest extends ListReaderCustomizerTest<Nat64Prefixes, Nat64PrefixesKey, Nat64PrefixesBuilder> {

    @Mock
    private FutureJVppNatFacade jvppNat;

    public Nat64PrefixesCustomizerTest() {
        super(Nat64Prefixes.class, PolicyBuilder.class);
    }

    @Override
    protected ReaderCustomizer<Nat64Prefixes, Nat64PrefixesBuilder> initCustomizer() {
        return new Nat64PrefixesCustomizer(jvppNat);
    }

    @Test
    public void testGetAllNoPrefixes() throws ReadFailedException {
        when(jvppNat.nat64PrefixDump(any())).thenReturn(future(dump()));
        final List<Nat64PrefixesKey> allIds = getCustomizer().getAllIds(getWildcardedId(123), ctx);
        assertEquals(0, allIds.size());
    }

    @Test
    public void testGetAll() throws ReadFailedException {
        when(jvppNat.nat64PrefixDump(any())).thenReturn(future(dump()));
        final long vrfId = 0;
        final List<Nat64PrefixesKey> allIds = getCustomizer().getAllIds(getWildcardedId(vrfId), ctx);
        assertEquals(1, allIds.size());
        assertEquals(new Nat64PrefixesKey(new Ipv6Prefix("64:ff9b::1/96")), allIds.get(0));
    }

    @Test
    public void testReadMissingForGivenVrf() throws ReadFailedException {
        final long vrfId = 123;
        when(jvppNat.nat64PrefixDump(any())).thenReturn(future(dump()));
        final Nat64PrefixesBuilder builder = mock(Nat64PrefixesBuilder.class);
        getCustomizer().readCurrentAttributes(getId(vrfId, "::1/128"), builder, ctx);
        verifyZeroInteractions(builder);
    }

    @Test
    public void testRead() throws ReadFailedException {
        final long vrfId = 1;
        when(jvppNat.nat64PrefixDump(any())).thenReturn(future(dump()));
        final Nat64PrefixesBuilder builder = mock(Nat64PrefixesBuilder.class);
        getCustomizer().readCurrentAttributes(getId(vrfId, "::1/128"), builder, ctx);
        verify(builder).setNat64Prefix(new Ipv6Prefix("::1/128"));
    }

    private static InstanceIdentifier<Nat64Prefixes> getWildcardedId(final long vrfId) {
        return InstanceIdentifier.create(Instances.class)
                .child(Instance.class, new InstanceKey(vrfId))
                .child(Policy.class, new PolicyKey(0L))
                .child(Nat64Prefixes.class);
    }

    private static InstanceIdentifier<Nat64Prefixes> getId(final long vrfId, final String prefix) {
        return InstanceIdentifier.create(Instances.class)
                .child(Instance.class, new InstanceKey(vrfId))
            .child(Policy.class, new PolicyKey(0L))
                .child(Nat64Prefixes.class, new Nat64PrefixesKey(new Ipv6Prefix(prefix)));
    }

    private Nat64PrefixDetailsReplyDump dump() {
        final Nat64PrefixDetailsReplyDump reply = new Nat64PrefixDetailsReplyDump();
        final Nat64PrefixDetails prefix0 = new Nat64PrefixDetails();
        prefix0.vrfId = 0;
        prefix0.prefix = new Ip6Prefix();
        prefix0.prefix.prefix = new Ip6Address();
        prefix0.prefix.prefix.ip6Address =
                new byte[]{0, 0x64, (byte) 0xff, (byte) 0x9b, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
        prefix0.prefix.len = (byte) 96;
        reply.nat64PrefixDetails.add(prefix0);
        final Nat64PrefixDetails prefix1 = new Nat64PrefixDetails();
        prefix1.vrfId = 1;
        prefix1.prefix = new Ip6Prefix();
        prefix1.prefix.prefix = new Ip6Address();
        prefix1.prefix.prefix.ip6Address = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
        prefix1.prefix.len = (byte) 128;
        reply.nat64PrefixDetails.add(prefix1);
        return reply;
    }
}