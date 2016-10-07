package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.fd.honeycomb.lisp.translate.write.trait.SubtableWriterTestCase;
import io.fd.honeycomb.translate.vpp.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.VppCallbackException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.EidTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.VniTableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtable;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.eid.table.grouping.eid.table.vni.table.BridgeDomainSubtableBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class BridgeDomainCustomizerTest extends SubtableWriterTestCase {

    private BridgeDomainSubtableCustomizer customizer;
    private InstanceIdentifier<BridgeDomainSubtable> validId;
    private BridgeDomainSubtable validData;
    private NamingContext bridgeDomainContext;

    @Before
    public void init() {
        bridgeDomainContext = new NamingContext("br", "bridge-domain-context");
        customizer = new BridgeDomainSubtableCustomizer(api, bridgeDomainContext);
        validId = InstanceIdentifier.create(EidTable.class).child(VniTable.class, new VniTableKey(12L))
                .child(BridgeDomainSubtable.class);
        validData = new BridgeDomainSubtableBuilder().setBridgeDomainRef("br-domain").build();
        defineMapping(mappingContext, "br-domain", 10, "bridge-domain-context");
    }

    @Test
    public void testWriteSuccessfull() throws WriteFailedException {
        whenAddDelEidTableAddDelMapSuccess();
        customizer.writeCurrentAttributes(validId, validData, writeContext);
        verifyAddDelEidTableAddDelMapInvokedCorrectly(1, 12, 10, 1);
    }

    @Test
    public void testWriteFailed() throws WriteFailedException {
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
        verifyAddDelEidTableAddDelMapInvokedCorrectly(0, 12, 10, 1);
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
