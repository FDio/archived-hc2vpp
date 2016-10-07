package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.LispStateBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.ShowLispStatusReply;

public class LispStateCustomizerTest extends ReaderCustomizerTest<LispState, LispStateBuilder> {

    private InstanceIdentifier<LispState> identifier;

    public LispStateCustomizerTest() {
        super(LispState.class, null);
    }

    @Before
    public void init() {
        identifier = InstanceIdentifier.create(LispState.class);
        final ShowLispStatusReply reply = new ShowLispStatusReply();
        reply.featureStatus = 1;

        when(api.showLispStatus(Mockito.any())).thenReturn(future(reply));
    }

    @Test
    public void testReadCurrentAttributes() throws ReadFailedException {

        LispStateBuilder builder = new LispStateBuilder();
        getCustomizer().readCurrentAttributes(identifier, builder, ctx);

        assertEquals(true, builder.build().isEnable());
    }

    @Override
    protected ReaderCustomizer<LispState, LispStateBuilder> initCustomizer() {
        return new LispStateCustomizer(api);
    }

    @Override
    public void testMerge() throws Exception {
        //LispState is root node, so there is no way to implement merge(it is also not needed)
    }
}
