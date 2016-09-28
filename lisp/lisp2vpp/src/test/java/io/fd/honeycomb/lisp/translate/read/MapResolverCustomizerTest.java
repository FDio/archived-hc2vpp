package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4AddressNoZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.MapResolvers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.MapResolversBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolverBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.map.resolvers.grouping.map.resolvers.MapResolverKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDetails;
import io.fd.vpp.jvpp.core.dto.LispMapResolverDetailsReplyDump;


public class MapResolverCustomizerTest
        extends ListReaderCustomizerTest<MapResolver, MapResolverKey, MapResolverBuilder> {

    private static final IpAddress IP_ADDRESS = new IpAddress(new Ipv4AddressNoZone("192.168.2.1"));
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
        assertEquals("192.168.2.1", new String(key.getIpAddress().getValue()));

    }

    private void defineDumpData() {
        final LispMapResolverDetailsReplyDump replyDump = new LispMapResolverDetailsReplyDump();
        final LispMapResolverDetails detail = new LispMapResolverDetails();
        detail.context = 5;
        detail.ipAddress = new byte[]{1, 2, -88, -64};
        detail.isIpv6 = 0;

        replyDump.lispMapResolverDetails = ImmutableList.of(detail);

        when(api.lispMapResolverDump(any())).thenReturn(future(replyDump));
    }

    @Override
    protected ReaderCustomizer<MapResolver, MapResolverBuilder> initCustomizer() {
        return new MapResolverCustomizer(api);
    }
}