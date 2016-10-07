package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.fd.honeycomb.lisp.translate.write.trait.SubtableWriterTestCase;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppCallbackException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.VrfSubtableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class VrfSubtableCustomizerTest extends SubtableWriterTestCase {

    private VrfSubtableCustomizer customizer;
    private InstanceIdentifier<VrfSubtable> validId;
    private VrfSubtable validData;

    @Before
    public void init() {
        customizer = new VrfSubtableCustomizer(api);
        validId = InstanceIdentifier.create(EidTable.class).child(VniTable.class, new VniTableKey(12L))
                .child(VrfSubtable.class);
        validData = new VrfSubtableBuilder().setTableId(10L).build();
    }

    @Test
    public void testWriteSuccessfull() throws WriteFailedException {
        whenAddDelEidTableAddDelMapSuccess();

        customizer.writeCurrentAttributes(validId, validData, writeContext);
        verifyAddDelEidTableAddDelMapInvokedCorrectly(1, 12, 10, 0);
    }

    @Test
    public void testWriteFailed() {
        whenAddDelEidTableAddDelMapFail();

        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof WriteFailedException);

            final WriteFailedException realException = ((WriteFailedException) e);
            assertEquals(validId, realException.getFailedId());
            assertTrue(e.getCause() instanceof VppCallbackException);
            return;
        }

        fail("Test should throw exception");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdate() throws WriteFailedException {
        customizer.updateCurrentAttributes(validId, validData, validData, writeContext);
    }

    @Test
    public void testDeleteSuccessfull() throws WriteFailedException {
        whenAddDelEidTableAddDelMapSuccess();

        customizer.deleteCurrentAttributes(validId, validData, writeContext);
        verifyAddDelEidTableAddDelMapInvokedCorrectly(0, 12, 10, 0);
    }

    @Test
    public void testDeleteFailed() {
        whenAddDelEidTableAddDelMapFail();

        try {
            customizer.deleteCurrentAttributes(validId, validData, writeContext);
        } catch (Exception e) {
            assertTrue(e instanceof WriteFailedException);

            final WriteFailedException realException = ((WriteFailedException) e);
            assertEquals(validId, realException.getFailedId());
            assertTrue(e.getCause() instanceof VppCallbackException);
            return;
        }

        fail("Test should throw exception");
    }
}
