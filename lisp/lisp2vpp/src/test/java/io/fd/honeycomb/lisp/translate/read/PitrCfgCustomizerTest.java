package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.LispStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.pitr.cfg.grouping.PitrCfg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.pitr.cfg.grouping.PitrCfgBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import io.fd.vpp.jvpp.core.dto.ShowLispPitrReply;


public class PitrCfgCustomizerTest extends ReaderCustomizerTest<PitrCfg, PitrCfgBuilder> {

    private static final byte[] LOC_SET_NAME_BYTES = "loc-set".getBytes(StandardCharsets.UTF_8);

    private InstanceIdentifier<PitrCfg> emptyId;
    private PitrCfg validData;

    public PitrCfgCustomizerTest() {
        super(PitrCfg.class, LispStateBuilder.class);
    }

    @Before
    public void init() {
        emptyId = InstanceIdentifier.create(PitrCfg.class);
        validData = new PitrCfgBuilder().setLocatorSet("loc-set").build();

        mockDumpData();
    }

    @Test
    public void readCurrentAttributes() throws Exception {
        PitrCfgBuilder builder = new PitrCfgBuilder();
        getCustomizer().readCurrentAttributes(emptyId, builder, ctx);

        final PitrCfg cfg = builder.build();

        assertNotNull(cfg);
        assertEquals("loc-set", cfg.getLocatorSet());
    }

    private void mockDumpData() {
        ShowLispPitrReply replyDump = new ShowLispPitrReply();
        replyDump.locatorSetName = LOC_SET_NAME_BYTES;
        replyDump.status = 1;

        when(api.showLispPitr(any())).thenReturn(future(replyDump));
    }

    @Override
    protected ReaderCustomizer<PitrCfg, PitrCfgBuilder> initCustomizer() {
        return new PitrCfgCustomizer(api);
    }
}