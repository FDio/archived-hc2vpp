package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ListReaderCustomizerTest;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.InterfaceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispLocatorDetails;
import org.openvpp.jvpp.core.dto.LispLocatorDetailsReplyDump;

public class InterfaceCustomizerTest
        extends ListReaderCustomizerTest<Interface, InterfaceKey, InterfaceBuilder> {

    public InterfaceCustomizerTest() {
        super(Interface.class, LocatorSetBuilder.class);
    }

    private InstanceIdentifier<Interface> validId;

    @Before
    public void init() {
        validId = InstanceIdentifier.create(LocatorSets.class).child(LocatorSet.class, new LocatorSetKey("loc-set-1"))
                .child(Interface.class, new InterfaceKey("interface-1"));

        //mappings
        defineMappings();
        //dump data
        defineDumpData();
    }

    private void defineDumpData() {
        final LispLocatorDetailsReplyDump dump = new LispLocatorDetailsReplyDump();

        final LispLocatorDetails detail1 = new LispLocatorDetails();
        detail1.swIfIndex = 1;
        detail1.ipAddress = new byte[]{-64, -88, 2, 1};
        detail1.isIpv6 = 0;
        detail1.local = 0;
        detail1.priority = 1;
        detail1.weight = 2;

        final LispLocatorDetails detail2 = new LispLocatorDetails();
        detail2.swIfIndex = 2;
        detail2.ipAddress = new byte[]{-64, -88, 2, 2};
        detail2.isIpv6 = 0;
        detail2.local = 0;
        detail2.priority = 2;
        detail2.weight = 3;

        dump.lispLocatorDetails = ImmutableList.of(detail1, detail2);

        when(api.lispLocatorDump(Mockito.any())).thenReturn(future(dump));
    }

    private void defineMappings() {
        defineMapping(mappingContext, "interface-1", 1, "interface-context");
        defineMapping(mappingContext, "interface-2", 2, "interface-context");
        defineMapping(mappingContext, "loc-set-1", 3, "locator-set-context");
    }

    @Test
    public void testGetAllIds() throws ReadFailedException {

        final List<InterfaceKey> keys = getCustomizer().getAllIds(validId, ctx);

        assertEquals(2, keys.size());
        assertEquals("interface-1", keys.get(0).getInterfaceRef());
        assertEquals("interface-2", keys.get(1).getInterfaceRef());
    }

    @Test
    public void testReadCurrentAttributes() throws ReadFailedException {
        InterfaceBuilder builder = new InterfaceBuilder();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        final Interface iface = builder.build();
        assertEquals("interface-1", iface.getInterfaceRef());
        assertEquals("interface-1", iface.getKey().getInterfaceRef());

    }

    @Override
    protected ReaderCustomizer<Interface, InterfaceBuilder> initCustomizer() {
        return new InterfaceCustomizer(api, new NamingContext("interface", "interface-context"),
                new NamingContext("loc-set", "locator-set-context"));
    }
}
