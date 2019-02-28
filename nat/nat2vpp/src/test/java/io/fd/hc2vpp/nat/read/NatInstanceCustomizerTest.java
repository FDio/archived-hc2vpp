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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.jvpp.nat.dto.Nat44StaticMappingDetails;
import io.fd.jvpp.nat.dto.Nat44StaticMappingDetailsReplyDump;
import io.fd.jvpp.nat.dto.Nat64BibDetails;
import io.fd.jvpp.nat.dto.Nat64BibDetailsReplyDump;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.Instances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.InstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.Instance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev180628.nat.instances.InstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class NatInstanceCustomizerTest
        extends InitializingListReaderCustomizerTest<Instance, InstanceKey, InstanceBuilder> {
    @Mock
    private EntityDumpExecutor<Nat44StaticMappingDetailsReplyDump, Void> nat44DumpExecutor;
    @Mock
    private EntityDumpExecutor<Nat64BibDetailsReplyDump, Void> nat64DumpExecutor;

    private KeyedInstanceIdentifier<Instance, InstanceKey> natInstanceId;
    private InstanceIdentifier<Instance> natInstanceWildcarded;
    private DumpCacheManager<Nat44StaticMappingDetailsReplyDump, Void> mapEntryNat44DumpMgr;
    private DumpCacheManager<Nat64BibDetailsReplyDump, Void> mapEntryNat64DumpMgr;

    public NatInstanceCustomizerTest() {
        super(Instance.class, InstancesBuilder.class);
    }

    @Override
    protected NatInstanceCustomizer initCustomizer() {
        return new NatInstanceCustomizer(mapEntryNat44DumpMgr, mapEntryNat64DumpMgr);
    }

    @Override
    protected void setUp() throws Exception {
        natInstanceId = InstanceIdentifier.create(Instances.class)
                .child(Instance.class, new InstanceKey(NatInstanceCustomizer.DEFAULT_VRF_ID));
        natInstanceWildcarded = InstanceIdentifier.create(Instances.class)
                .child(Instance.class);
        mapEntryNat44DumpMgr = new DumpCacheManager.DumpCacheManagerBuilder<Nat44StaticMappingDetailsReplyDump, Void>()
                .withExecutor(nat44DumpExecutor)
                .acceptOnly(Nat44StaticMappingDetailsReplyDump.class)
                .build();
        mapEntryNat64DumpMgr = new DumpCacheManager.DumpCacheManagerBuilder<Nat64BibDetailsReplyDump, Void>()
                .withExecutor(nat64DumpExecutor)
                .acceptOnly(Nat64BibDetailsReplyDump.class)
                .build();
    }

    @Test
    public void testRead() throws ReadFailedException {
        final InstanceBuilder builder = mock(InstanceBuilder.class);
        getCustomizer().readCurrentAttributes(natInstanceId, builder, ctx);
        verify(builder).setId(natInstanceId.getKey().getId());
    }

    @Test
    public void testReadAll() throws ReadFailedException {
        when(nat44DumpExecutor.executeDump(natInstanceWildcarded, null)).thenReturn(nat44NonEmptyDump());
        when(nat64DumpExecutor.executeDump(natInstanceWildcarded, null)).thenReturn(nat64NonEmptyDump());
        final List<InstanceKey> allIds = getCustomizer().getAllIds(natInstanceWildcarded, ctx);
        assertThat(allIds, hasSize(6));
        assertThat(allIds, hasItems(
                new InstanceKey(0L), new InstanceKey(1L), new InstanceKey(2L), new InstanceKey(3L),
                new InstanceKey(5L), new InstanceKey(6L)));
    }

    private static Nat44StaticMappingDetailsReplyDump nat44NonEmptyDump() {
        Nat44StaticMappingDetailsReplyDump replyDump = new Nat44StaticMappingDetailsReplyDump();
        Nat44StaticMappingDetails detailsOne = new Nat44StaticMappingDetails();
        detailsOne.vrfId = 1;

        Nat44StaticMappingDetails detailsTwo = new Nat44StaticMappingDetails();
        detailsTwo.vrfId = 2;

        Nat44StaticMappingDetails detailsThree = new Nat44StaticMappingDetails();
        detailsThree.vrfId = 3;

        replyDump.nat44StaticMappingDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);
        return replyDump;
    }

    private static Nat64BibDetailsReplyDump nat64NonEmptyDump() {
        Nat64BibDetailsReplyDump replyDump = new Nat64BibDetailsReplyDump();
        Nat64BibDetails detailsOne = new Nat64BibDetails();
        detailsOne.vrfId = 2;

        Nat64BibDetails detailsTwo = new Nat64BibDetails();
        detailsTwo.vrfId = 5;

        Nat64BibDetails detailsThree = new Nat64BibDetails();
        detailsThree.vrfId = 6;

        replyDump.nat64BibDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);
        return replyDump;
    }
}