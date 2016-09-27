package io.fd.honeycomb.translate.v3po.interfacesstate;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.SubinterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.SubInterfaces;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.interfaces.state._interface.sub.interfaces.SubInterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.vlan.rev150527.sub._interface.base.attributes.L2Builder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class SubInterfaceL2CustomizerTest extends ReaderCustomizerTest<L2, L2Builder> {

    private static final String IFC_CTX_NAME = "ifc-test-instance";
    private static final String BD_CTX_NAME = "bd-test-instance";
    private NamingContext interfaceContext;
    private NamingContext bridgeDomainContext;

    private static final String IF_NAME = "local0";
    private static final int IF_INDEX = 1;
    private static final String SUB_IF_NAME = "local0.1";
    private static final long SUB_IF_ID = 1;
    private static final int SUB_IF_INDEX = 11;
    private InstanceIdentifier<L2> IID =
        InstanceIdentifier.create(InterfacesState.class).child(Interface.class, new InterfaceKey(IF_NAME))
            .augmentation(SubinterfaceStateAugmentation.class)
            .child(SubInterfaces.class).child(SubInterface.class, new SubInterfaceKey(SUB_IF_ID)).child(L2.class);

    public SubInterfaceL2CustomizerTest() {
        super(L2.class, SubInterfaceBuilder.class);
    }

    @Override
    protected void setUp() {
        interfaceContext = new NamingContext("generatedIfaceName", IFC_CTX_NAME);
        bridgeDomainContext = new NamingContext("generatedBDName", BD_CTX_NAME);
        defineMapping(mappingContext, IF_NAME, IF_INDEX, IFC_CTX_NAME);
        defineMapping(mappingContext, SUB_IF_NAME, SUB_IF_INDEX, IFC_CTX_NAME);
    }

    @Override
    protected ReaderCustomizer<L2, L2Builder> initCustomizer() {
        return new SubInterfaceL2Customizer(api, interfaceContext, bridgeDomainContext);
    }

    @Test(expected = ReadFailedException.class)
    public void testReadFailed() throws ReadFailedException {
        final L2Builder builder = mock(L2Builder.class);
        when(api.swInterfaceDump(any())).thenReturn(failedFuture());
        getCustomizer().readCurrentAttributes(IID, builder, ctx);
    }
}