package io.fd.honeycomb.lisp.translate.read;


import static io.fd.honeycomb.lisp.translate.read.dump.executor.params.SubtableDumpParams.MapLevel.L2;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.fd.honeycomb.lisp.translate.read.trait.SubtableReaderTestCase;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.vpp.jvpp.VppCallbackException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BridgeDomainSubtableCustomizerTest
        extends SubtableReaderTestCase<BridgeDomainSubtable, BridgeDomainSubtableBuilder> {

    private InstanceIdentifier<BridgeDomainSubtable> validId;
    private NamingContext bridgeDomainContext;

    public BridgeDomainSubtableCustomizerTest() {
        super(BridgeDomainSubtable.class, VniTableBuilder.class);
    }

    @Override
    protected void setUp() throws Exception {
        bridgeDomainContext = new NamingContext("br", "br-domain-context");
        validId = InstanceIdentifier.create(EidTable.class).child(VniTable.class, new VniTableKey(expectedVni))
                .child(BridgeDomainSubtable.class);

        defineMapping(mappingContext, "br-domain", expectedTableId, "br-domain-context");
    }

    @Test
    public void testReadCurrentSuccessfull() throws ReadFailedException {
        doReturnValidNonEmptyDataOnDump();
        BridgeDomainSubtableBuilder builder = new BridgeDomainSubtableBuilder();
        customizer.readCurrentAttributes(validId, builder, ctx);

        verifyLispEidTableMapDumpCalled(L2);

        final BridgeDomainSubtable subtable = builder.build();
        assertNotNull(subtable);
        assertEquals("br-domain", subtable.getBridgeDomainRef());
    }


    @Test
    public void testReadCurrentEmptyDump() throws ReadFailedException {
        doReturnEmptyDataOnDump();
        BridgeDomainSubtableBuilder builder = new BridgeDomainSubtableBuilder();
        customizer.readCurrentAttributes(validId, builder, ctx);

        verifyLispEidTableMapDumpCalled(L2);

        final BridgeDomainSubtable subtable = builder.build();
        assertNotNull(subtable);
        assertNull(subtable.getBridgeDomainRef());
    }

    @Test
    public void testReadCurrentFailed() {
        doThrowOnDump();
        BridgeDomainSubtableBuilder builder = new BridgeDomainSubtableBuilder();
        try {
            customizer.readCurrentAttributes(validId, builder, ctx);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            assertNull(builder.getBridgeDomainRef());
            verifyLispEidTableMapDumpNotCalled();

            return;
        }

        fail("Test should throw ReadFailedException");
    }

    @Test
    public void testGetBuilder() {
        final BridgeDomainSubtableBuilder builder = customizer.getBuilder(validId);

        assertNotNull(builder);
        assertNull(builder.getLocalMappings());
        assertNull(builder.getRemoteMappings());
        assertNull(builder.getBridgeDomainRef());
    }

    @Override
    protected ReaderCustomizer<BridgeDomainSubtable, BridgeDomainSubtableBuilder> initCustomizer() {
        return new BridgeDomainSubtableCustomizer(api, bridgeDomainContext);
    }
}
