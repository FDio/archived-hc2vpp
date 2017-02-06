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
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingListReaderCustomizerTest;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetails;
import io.fd.vpp.jvpp.snat.dto.SnatStaticMappingDetailsReplyDump;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.NatInstancesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstanceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstanceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class NatInstanceCustomizerTest
    extends InitializingListReaderCustomizerTest<NatInstance, NatInstanceKey, NatInstanceBuilder> {
    @Mock
    private EntityDumpExecutor<SnatStaticMappingDetailsReplyDump, Void> dumpExecutor;

    private KeyedInstanceIdentifier<NatInstance, NatInstanceKey> natInstanceId;
    private InstanceIdentifier<NatInstance> natInstanceWildcarded;
    private DumpCacheManager<SnatStaticMappingDetailsReplyDump, Void> dumpCacheManager;

    public NatInstanceCustomizerTest() {
        super(NatInstance.class, NatInstancesBuilder.class);
    }

    @Override
    protected NatInstanceCustomizer initCustomizer() {
        return new NatInstanceCustomizer(dumpCacheManager);
    }

    @Override
    protected void setUp() throws Exception {
        natInstanceId = InstanceIdentifier.create(NatInstances.class)
            .child(NatInstance.class, new NatInstanceKey(NatInstanceCustomizer.DEFAULT_VRF_ID));
        natInstanceWildcarded = InstanceIdentifier.create(NatInstances.class)
            .child(NatInstance.class);
        dumpCacheManager = new DumpCacheManager.DumpCacheManagerBuilder<SnatStaticMappingDetailsReplyDump, Void>()
            .withExecutor(dumpExecutor)
            .acceptOnly(SnatStaticMappingDetailsReplyDump.class)
            .build();
    }

    @Test
    public void testRead() throws ReadFailedException {
        final NatInstanceBuilder builder = mock(NatInstanceBuilder.class);
        getCustomizer().readCurrentAttributes(natInstanceId, builder, ctx);
        verify(builder).setId(natInstanceId.getKey().getId());
    }

    @Test
    public void testReadAll() throws ReadFailedException {
        final SnatStaticMappingDetailsReplyDump dump = dumpNonEmptyDefaultInstance();
        when(dumpExecutor.executeDump(natInstanceWildcarded, null)).thenReturn(dump);
        final List<NatInstanceKey> allIds = getCustomizer().getAllIds(natInstanceWildcarded, ctx);
        assertThat(allIds, hasItems(
            new NatInstanceKey(0L), new NatInstanceKey(1L), new NatInstanceKey(2L), new NatInstanceKey(3L)));
    }

    private static SnatStaticMappingDetailsReplyDump dumpNonEmptyDefaultInstance() {
        SnatStaticMappingDetailsReplyDump replyDump = new SnatStaticMappingDetailsReplyDump();
        SnatStaticMappingDetails detailsOne = new SnatStaticMappingDetails();
        detailsOne.vrfId = 1;

        SnatStaticMappingDetails detailsTwo = new SnatStaticMappingDetails();
        detailsTwo.vrfId = 2;

        SnatStaticMappingDetails detailsThree = new SnatStaticMappingDetails();
        detailsThree.vrfId = 3;

        replyDump.snatStaticMappingDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);
        return replyDump;
    }
}