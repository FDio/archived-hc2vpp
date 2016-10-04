package io.fd.honeycomb.lisp.translate.initializers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.lisp.feature.data.grouping.LispFeatureDataBuilder;


public class LispInitializerTest {
    @Test
    public void convert() throws Exception {

        final LispInitializer initializer = new LispInitializer(mock(DataBroker.class));
        final LispState state = new LispStateBuilder()
                .setEnable(true)
                .setLispFeatureData(new LispFeatureDataBuilder().build())
                .build();

        final Lisp operational = initializer.convert(state);

        assertNotNull(operational);
        assertEquals(operational.isEnable(), state.isEnable());
        assertEquals(operational.getLispFeatureData(), state.getLispFeatureData());
    }

}