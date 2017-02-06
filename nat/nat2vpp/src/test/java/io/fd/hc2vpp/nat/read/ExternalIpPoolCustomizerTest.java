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
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.util.read.cache.DumpCacheManager;
import io.fd.honeycomb.translate.util.read.cache.EntityDumpExecutor;
import io.fd.vpp.jvpp.snat.dto.SnatAddressDetails;
import io.fd.vpp.jvpp.snat.dto.SnatAddressDetailsReplyDump;
import java.util.Arrays;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.parameters.ExternalIpAddressPoolKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.NatInstances;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstance;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.NatInstanceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.NatCurrentConfig;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.nat.rev150908.nat.state.nat.instances.nat.instance.NatCurrentConfigBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ExternalIpPoolCustomizerTest
    extends ListReaderCustomizerTest<ExternalIpAddressPool, ExternalIpAddressPoolKey, ExternalIpAddressPoolBuilder> {

    private InstanceIdentifier<ExternalIpAddressPool> externalPoolIdDefaultNatInstance;
    private InstanceIdentifier<ExternalIpAddressPool> externalPoolIdDifferentNatInstance;
    private DumpCacheManager<SnatAddressDetailsReplyDump, Void> dumpCacheManager;

    @Mock
    private EntityDumpExecutor<SnatAddressDetailsReplyDump, Void> executor;

    public ExternalIpPoolCustomizerTest() {
        super(ExternalIpAddressPool.class, NatCurrentConfigBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        externalPoolIdDefaultNatInstance = InstanceIdentifier
            .create(NatInstances.class)
            .child(NatInstance.class, new NatInstanceKey(NatInstanceCustomizer.DEFAULT_VRF_ID))
            .child(NatCurrentConfig.class)
            .child(ExternalIpAddressPool.class, new ExternalIpAddressPoolKey(2L));

        externalPoolIdDifferentNatInstance = InstanceIdentifier
            .create(NatInstances.class)
            .child(NatInstance.class, new NatInstanceKey(7L))
            .child(NatCurrentConfig.class)
            .child(ExternalIpAddressPool.class, new ExternalIpAddressPoolKey(2L));

        dumpCacheManager = new DumpCacheManager.DumpCacheManagerBuilder<SnatAddressDetailsReplyDump, Void>()
            .withExecutor(executor)
            .acceptOnly(SnatAddressDetailsReplyDump.class)
            .build();
    }

    @Override
    protected ReaderCustomizer<ExternalIpAddressPool, ExternalIpAddressPoolBuilder> initCustomizer() {
        return new ExternalIpPoolCustomizer(dumpCacheManager);
    }

    @Test
    public void testReadAttributes() throws Exception {
        when(executor.executeDump(externalPoolIdDefaultNatInstance, null)).thenReturn(dumpReplyNonEmpty());

        final ExternalIpAddressPoolBuilder builder = new ExternalIpAddressPoolBuilder();
        getCustomizer().readCurrentAttributes(externalPoolIdDefaultNatInstance, builder, ctx);

        assertEquals("192.168.2.3/32", builder.getExternalIpPool().getValue());
        assertEquals(2L, builder.getPoolId().longValue());
    }

    @Test
    public void testGetAll() throws Exception {
        when(executor.executeDump(externalPoolIdDefaultNatInstance, null)).thenReturn(dumpReplyNonEmpty());

        final List<ExternalIpAddressPoolKey> allIds = getCustomizer().getAllIds(externalPoolIdDefaultNatInstance, ctx);
        assertThat(allIds, hasItems(
            LongStream.range(0, 2).mapToObj(ExternalIpAddressPoolKey::new).toArray(ExternalIpAddressPoolKey[]::new)));
    }

    @Test
    public void testGetAllDifferentInstance() throws Exception {
        assertThat(getCustomizer().getAllIds(externalPoolIdDifferentNatInstance, ctx), empty());
    }

    @Test
    public void testGetAllNoDump() throws Exception {
        when(executor.executeDump(externalPoolIdDefaultNatInstance, null)).thenReturn(dumpReplyEmpty());
        assertThat(getCustomizer().getAllIds(externalPoolIdDefaultNatInstance, ctx), empty());
    }

    private static SnatAddressDetailsReplyDump dumpReplyEmpty() {
        return new SnatAddressDetailsReplyDump();
    }

    private static SnatAddressDetailsReplyDump dumpReplyNonEmpty() {
        SnatAddressDetailsReplyDump replyDump = dumpReplyEmpty();

        SnatAddressDetails detailsOne = new SnatAddressDetails();
        detailsOne.ipAddress = new byte[] {-64, -88, 2, 1};
        detailsOne.isIp4 = 1;

        SnatAddressDetails detailsTwo = new SnatAddressDetails();
        detailsTwo.ipAddress = new byte[] {-64, -88, 2, 2};
        detailsTwo.isIp4 = 1;

        SnatAddressDetails detailsThree = new SnatAddressDetails();
        detailsThree.ipAddress = new byte[] {-64, -88, 2, 3};
        detailsThree.isIp4 = 1;

        replyDump.snatAddressDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);

        return replyDump;
    }
}