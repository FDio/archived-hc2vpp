package io.fd.honeycomb.lisp.translate.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.LispGetMapRequestItrRlocs;
import io.fd.vpp.jvpp.core.dto.LispGetMapRequestItrRlocsReply;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.itr.remote.locator.sets.grouping.ItrRemoteLocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class ItrRemoteLocatorSetCustomizerTest
        extends ReaderCustomizerTest<ItrRemoteLocatorSet, ItrRemoteLocatorSetBuilder> {

    private static final String EXPECTED_LOCATOR_SET_NAME = "loc-set";

    private InstanceIdentifier<ItrRemoteLocatorSet> validId;
    private ItrRemoteLocatorSetBuilder builder;

    public ItrRemoteLocatorSetCustomizerTest() {
        super(ItrRemoteLocatorSet.class, LispFeatureDataBuilder.class);
    }

    @Before
    public void setUp() throws Exception {
        validId = InstanceIdentifier.create(ItrRemoteLocatorSet.class);
        builder = new ItrRemoteLocatorSetBuilder();
    }

    @Override
    protected ReaderCustomizer<ItrRemoteLocatorSet, ItrRemoteLocatorSetBuilder> initCustomizer() {
        return new ItrRemoteLocatorSetCustomizer(api);
    }

    @Test
    public void getBuilder() throws Exception {
        final ItrRemoteLocatorSetBuilder itrRemoteLocatorSetBuilder = getCustomizer().getBuilder(validId);

        assertNotNull(itrRemoteLocatorSetBuilder);
        assertNull(itrRemoteLocatorSetBuilder.getRemoteLocatorSetName());
    }

    @Test
    public void readCurrentAttributesSuccess() throws Exception {
        doReturnValidDataOnDump();

        getCustomizer().readCurrentAttributes(validId, builder, ctx);

        assertNotNull(builder);
        assertEquals(EXPECTED_LOCATOR_SET_NAME, builder.getRemoteLocatorSetName());
        verifyLispGetMapRequestItrRlocsInvokedOnce();
    }

    @Test
    public void readCurrentAttributesEmptyData() throws Exception {
        doReturnEmptyDataOnDump();
        getCustomizer().readCurrentAttributes(validId, builder, ctx);
        verifyInvalidDataCase(builder);
    }

    @Test
    public void readCurrentAttributesFailedCallHalted() {
        doThrowExceptionOnDump();
        try {
            getCustomizer().readCurrentAttributes(validId, builder, ctx);
        } catch (ReadFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            assertNotNull(builder);
            assertNull(builder.getRemoteLocatorSetName());

            verifyLispGetMapRequestItrRlocsInvokedOnce();
            return;
        }

        fail("Test should have thrown exception");
    }

    @Test
    public void merge() throws Exception {
        LispFeatureDataBuilder builder = new LispFeatureDataBuilder();
        ItrRemoteLocatorSet set = new ItrRemoteLocatorSetBuilder().setRemoteLocatorSetName("loc-set").build();
        getCustomizer().merge(builder, set);

        assertNotNull(builder);
        assertEquals(set, builder.getItrRemoteLocatorSet());
    }


    private void doReturnValidDataOnDump() {
        LispGetMapRequestItrRlocsReply reply = new LispGetMapRequestItrRlocsReply();
        reply.locatorSetName = EXPECTED_LOCATOR_SET_NAME.getBytes(StandardCharsets.UTF_8);

        when(api.lispGetMapRequestItrRlocs(any(LispGetMapRequestItrRlocs.class)))
                .thenReturn(CompletableFuture.completedFuture(reply));
    }

    private void doReturnNullDataOnDump() {
        when(api.lispGetMapRequestItrRlocs(any(LispGetMapRequestItrRlocs.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private void doReturnEmptyDataOnDump() {
        when(api.lispGetMapRequestItrRlocs(any(LispGetMapRequestItrRlocs.class)))
                .thenReturn(CompletableFuture.completedFuture(new LispGetMapRequestItrRlocsReply()));
    }

    private void doThrowExceptionOnDump() {
        when(api.lispGetMapRequestItrRlocs(any(LispGetMapRequestItrRlocs.class))).
                thenReturn(new CompletableFuture<LispGetMapRequestItrRlocsReply>() {
                    @Override
                    public LispGetMapRequestItrRlocsReply get(final long l, final TimeUnit timeUnit)
                            throws InterruptedException, ExecutionException, TimeoutException {
                        throw new ExecutionException(new VppCallbackException("lispGetMapRequestItrRlocs", 1, -2));
                    }
                });
    }

    private void verifyLispGetMapRequestItrRlocsInvokedOnce() {
        verify(api, times(1)).lispGetMapRequestItrRlocs(any(LispGetMapRequestItrRlocs.class));
    }

    private void verifyInvalidDataCase(final ItrRemoteLocatorSetBuilder builder) {
        assertNotNull(builder);
        assertNull(builder.getRemoteLocatorSetName());

        verifyLispGetMapRequestItrRlocsInvokedOnce();
    }
}