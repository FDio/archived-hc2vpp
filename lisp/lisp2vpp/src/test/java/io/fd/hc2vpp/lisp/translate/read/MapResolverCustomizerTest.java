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

package io.fd.hc2vpp.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.OneMapResolverDetails;
import io.fd.vpp.jvpp.core.dto.OneMapResolverDetailsReplyDump;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.MapResolvers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.MapResolversBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.map.resolvers.MapResolverBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev171013.map.resolvers.grouping.map.resolvers.MapResolverKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class MapResolverCustomizerTest
        extends LispInitializingListReaderCustomizerTest<MapResolver, MapResolverKey, MapResolverBuilder> {

    private static final IpAddress IP_ADDRESS_REVERTED =
            new IpAddress(new Ipv4AddressNoZone("1.2.168.192"));

    private InstanceIdentifier<MapResolver> emptyId;
    private InstanceIdentifier<MapResolver> validId;

    public MapResolverCustomizerTest() {
        super(MapResolver.class, MapResolversBuilder.class);
    }

    @Before
    public void init() {

        emptyId = InstanceIdentifier.create(MapResolver.class);
        validId = InstanceIdentifier.create(MapResolvers.class)
                .child(MapResolver.class, new MapResolverKey(IP_ADDRESS_REVERTED));
        defineDumpData();
        mockLispEnabled();
    }

    @Test
    public void readCurrentAttributes() throws Exception {
        MapResolverBuilder builder = new MapResolverBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        MapResolver resolver = builder.build();
        assertNotNull(resolver);
        assertEquals("1.2.168.192", resolver.getIpAddress().getIpv4Address().getValue());
    }

    @Test
    public void getAllIds() throws Exception {
        final List<MapResolverKey> keys = getCustomizer().getAllIds(emptyId, ctx);

        assertEquals(1, keys.size());

        final MapResolverKey key = keys.get(0);
        assertNotNull(key);
        assertEquals("1.2.168.192", new String(key.getIpAddress().getValue()));

    }

    private void defineDumpData() {
        final OneMapResolverDetailsReplyDump replyDump = new OneMapResolverDetailsReplyDump();
        final OneMapResolverDetails detail = new OneMapResolverDetails();
        detail.context = 5;
        detail.ipAddress = new byte[]{1, 2, -88, -64};
        detail.isIpv6 = 0;

        replyDump.oneMapResolverDetails = ImmutableList.of(detail);

        when(api.oneMapResolverDump(any())).thenReturn(future(replyDump));
    }

    @Override
    protected ReaderCustomizer<MapResolver, MapResolverBuilder> initCustomizer() {
        return new MapResolverCustomizer(api, lispStateCheckService);
    }
}