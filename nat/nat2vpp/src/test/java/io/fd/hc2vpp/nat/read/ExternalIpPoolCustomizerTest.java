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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nat.rev170804.NatPoolType.Nat44;
import static org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nat.rev170804.NatPoolType.Nat64;

import io.fd.hc2vpp.common.test.read.ListReaderCustomizerTest;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.snat.dto.Nat64PoolAddrDetails;
import io.fd.vpp.jvpp.snat.dto.Nat64PoolAddrDetailsReplyDump;
import io.fd.vpp.jvpp.snat.dto.SnatAddressDetails;
import io.fd.vpp.jvpp.snat.dto.SnatAddressDetailsReplyDump;
import io.fd.vpp.jvpp.snat.future.FutureJVppSnatFacade;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.nat.rev170804.ExternalIpAddressPoolStateAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ExternalIpPoolCustomizerTest
    extends ListReaderCustomizerTest<ExternalIpAddressPool, ExternalIpAddressPoolKey, ExternalIpAddressPoolBuilder> {

    private static final InstanceIdentifier<NatCurrentConfig> NAT_CONFIG_ID =
            InstanceIdentifier.create(NatInstances.class)
                    .child(NatInstance.class, new NatInstanceKey(NatInstanceCustomizer.DEFAULT_VRF_ID))
                    .child(NatCurrentConfig.class);

    private static final InstanceIdentifier<ExternalIpAddressPool> NAT_DEFAULT_POOL_WILDCARDED_ID =
            NAT_CONFIG_ID.child(ExternalIpAddressPool.class);

    private static final InstanceIdentifier<ExternalIpAddressPool> NAT_NON_DEFAULT_POOL_WILDCARDED_ID =
            InstanceIdentifier.create(NatInstances.class).child(NatInstance.class, new NatInstanceKey(7L))
                    .child(NatCurrentConfig.class).child(ExternalIpAddressPool.class);

    @Mock
    private FutureJVppSnatFacade jvppSnat;

    public ExternalIpPoolCustomizerTest() {
        super(ExternalIpAddressPool.class, NatCurrentConfigBuilder.class);
    }

    @Override
    protected ReaderCustomizer<ExternalIpAddressPool, ExternalIpAddressPoolBuilder> initCustomizer() {
        return new ExternalIpPoolCustomizer(jvppSnat);
    }

    @Test
    public void testReadAttributesNat44() throws Exception {
        when(jvppSnat.snatAddressDump(any())).thenReturn(future(dumpReplyNat44NonEmpty()));
        final long poolId = 2;
        final ExternalIpAddressPoolBuilder builder = new ExternalIpAddressPoolBuilder();
        getCustomizer().readCurrentAttributes(getId(poolId), builder, ctx);

        assertEquals("192.168.44.3/32", builder.getExternalIpPool().getValue());
        assertEquals(poolId, builder.getPoolId().longValue());
        assertEquals(Nat44, builder.getAugmentation(ExternalIpAddressPoolStateAugmentation.class).getPoolType());
    }

    @Test
    public void testReadAttributesNat64() throws Exception {
        when(jvppSnat.snatAddressDump(any())).thenReturn(future(dumpReplyNat44Empty()));
        when(jvppSnat.nat64PoolAddrDump(any())).thenReturn(future(dumpReplyNat64NonEmpty()));
        final long poolId = 2;

        final ExternalIpAddressPoolBuilder builder = new ExternalIpAddressPoolBuilder();
        getCustomizer().readCurrentAttributes(getId(poolId), builder, ctx);

        assertEquals("192.168.64.3/32", builder.getExternalIpPool().getValue());
        assertEquals(poolId, builder.getPoolId().longValue());
        assertEquals(Nat64, builder.getAugmentation(ExternalIpAddressPoolStateAugmentation.class).getPoolType());
    }

    @Test
    public void testReadAttributes() throws Exception {
        when(jvppSnat.snatAddressDump(any())).thenReturn(future(dumpReplyNat44NonEmpty()));
        when(jvppSnat.nat64PoolAddrDump(any())).thenReturn(future(dumpReplyNat64NonEmpty()));
        final long poolId = 5;

        final ExternalIpAddressPoolBuilder builder = new ExternalIpAddressPoolBuilder();
        getCustomizer().readCurrentAttributes(getId(poolId), builder, ctx);

        assertEquals("192.168.64.3/32", builder.getExternalIpPool().getValue());
        assertEquals(poolId, builder.getPoolId().longValue());
        assertEquals(Nat64, builder.getAugmentation(ExternalIpAddressPoolStateAugmentation.class).getPoolType());
    }

    @Test
    public void testGetAllNat44() throws Exception {
        when(jvppSnat.snatAddressDump(any())).thenReturn(future(dumpReplyNat44NonEmpty()));
        when(jvppSnat.nat64PoolAddrDump(any())).thenReturn(future(dumpReplyNat64Empty()));

        final List<ExternalIpAddressPoolKey> allIds = getCustomizer().getAllIds(NAT_DEFAULT_POOL_WILDCARDED_ID, ctx);
        assertThat(allIds, hasItems(
                LongStream.range(0, 2).mapToObj(ExternalIpAddressPoolKey::new)
                        .toArray(ExternalIpAddressPoolKey[]::new)));
    }

    @Test
    public void testGetAllNat64() throws Exception {
        when(jvppSnat.snatAddressDump(any())).thenReturn(future(dumpReplyNat44Empty()));
        when(jvppSnat.nat64PoolAddrDump(any())).thenReturn(future(dumpReplyNat64NonEmpty()));

        final List<ExternalIpAddressPoolKey> allIds = getCustomizer().getAllIds(NAT_DEFAULT_POOL_WILDCARDED_ID, ctx);
        assertThat(allIds, hasItems(
                LongStream.range(0, 2).mapToObj(ExternalIpAddressPoolKey::new)
                        .toArray(ExternalIpAddressPoolKey[]::new)));
    }

    @Test
    public void testGetAll() throws Exception {
        when(jvppSnat.snatAddressDump(any())).thenReturn(future(dumpReplyNat44NonEmpty()));
        when(jvppSnat.nat64PoolAddrDump(any())).thenReturn(future(dumpReplyNat64NonEmpty()));

        final List<ExternalIpAddressPoolKey> allIds = getCustomizer().getAllIds(NAT_DEFAULT_POOL_WILDCARDED_ID, ctx);
        assertThat(allIds, hasItems(
                LongStream.range(0, 5).mapToObj(ExternalIpAddressPoolKey::new)
                        .toArray(ExternalIpAddressPoolKey[]::new)));
    }

    @Test
    public void testGetAllDifferentInstance() throws Exception {
        assertThat(getCustomizer().getAllIds(NAT_NON_DEFAULT_POOL_WILDCARDED_ID, ctx), empty());
    }

    @Test
    public void testGetAllNoDump() throws Exception {
        when(jvppSnat.snatAddressDump(any())).thenReturn(future(dumpReplyNat44Empty()));
        when(jvppSnat.nat64PoolAddrDump(any())).thenReturn(future(dumpReplyNat64Empty()));
        assertThat(getCustomizer().getAllIds(NAT_DEFAULT_POOL_WILDCARDED_ID, ctx), empty());
    }

    private static InstanceIdentifier<ExternalIpAddressPool> getId(final long id) {
        return NAT_CONFIG_ID.child(ExternalIpAddressPool.class, new ExternalIpAddressPoolKey(id));
    }

    private static SnatAddressDetailsReplyDump dumpReplyNat44Empty() {
        return new SnatAddressDetailsReplyDump();
    }

    private static SnatAddressDetailsReplyDump dumpReplyNat44NonEmpty() {
        SnatAddressDetailsReplyDump replyDump = dumpReplyNat44Empty();

        SnatAddressDetails detailsOne = new SnatAddressDetails();
        detailsOne.ipAddress = new byte[]{-64, -88, 44, 1};
        detailsOne.isIp4 = 1;

        SnatAddressDetails detailsTwo = new SnatAddressDetails();
        detailsTwo.ipAddress = new byte[]{-64, -88, 44, 2};
        detailsTwo.isIp4 = 1;

        SnatAddressDetails detailsThree = new SnatAddressDetails();
        detailsThree.ipAddress = new byte[]{-64, -88, 44, 3};
        detailsThree.isIp4 = 1;

        replyDump.snatAddressDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);

        return replyDump;
    }

    private static Nat64PoolAddrDetailsReplyDump dumpReplyNat64Empty() {
        return new Nat64PoolAddrDetailsReplyDump();
    }

    private static Nat64PoolAddrDetailsReplyDump dumpReplyNat64NonEmpty() {
        Nat64PoolAddrDetailsReplyDump replyDump = dumpReplyNat64Empty();

        Nat64PoolAddrDetails detailsOne = new Nat64PoolAddrDetails();
        detailsOne.address = new byte[]{-64, -88, 64, 1};

        Nat64PoolAddrDetails detailsTwo = new Nat64PoolAddrDetails();
        detailsTwo.address = new byte[]{-64, -88, 64, 2};

        Nat64PoolAddrDetails detailsThree = new Nat64PoolAddrDetails();
        detailsThree.address = new byte[]{-64, -88, 64, 3};

        replyDump.nat64PoolAddrDetails = Arrays.asList(detailsOne, detailsTwo, detailsThree);

        return replyDump;
    }
}