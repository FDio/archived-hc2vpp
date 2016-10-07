package io.fd.honeycomb.translate.v3po.interfacesstate.pbb;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.PbbRewriteStateInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.interfaces.state._interface.PbbRewriteState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.vpp.pbb.rev161214.interfaces.state._interface.PbbRewriteStateBuilder;

public class PbbRewriteStateCustomizerTest extends ReaderCustomizerTest<PbbRewriteState, PbbRewriteStateBuilder> {

    public PbbRewriteStateCustomizerTest() {
        super(PbbRewriteState.class, PbbRewriteStateInterfaceAugmentationBuilder.class);
    }

    @Override
    protected ReaderCustomizer<PbbRewriteState, PbbRewriteStateBuilder> initCustomizer() {
        return new PbbRewriteStateCustomizer(api);
    }
}
