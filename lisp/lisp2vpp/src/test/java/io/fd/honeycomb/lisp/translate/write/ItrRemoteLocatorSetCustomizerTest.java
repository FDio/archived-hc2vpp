package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.translate.vpp.util.ByteDataTranslator;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.honeycomb.vpp.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.LispAddDelMapRequestItrRlocs;
import io.fd.vpp.jvpp.core.dto.LispAddDelMapRequestItrRlocsReply;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev161214.itr.remote.locator.sets.grouping.ItrRemoteLocatorSetBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ItrRemoteLocatorSetCustomizerTest extends WriterCustomizerTest implements ByteDataTranslator {

    private static final String VALID_NAME = "loc-set";

    @Captor
    private ArgumentCaptor<LispAddDelMapRequestItrRlocs> requestCaptor;

    private ItrRemoteLocatorSetCustomizer customizer;
    private InstanceIdentifier<ItrRemoteLocatorSet> validId;
    private ItrRemoteLocatorSet validData;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        customizer = new ItrRemoteLocatorSetCustomizer(api);
        validId = InstanceIdentifier.create(ItrRemoteLocatorSet.class);
        validData = new ItrRemoteLocatorSetBuilder().setRemoteLocatorSetName(VALID_NAME).build();
    }

    @Test
    public void writeCurrentAttributesSuccess() throws Exception {
        onWriteSuccess();
        customizer.writeCurrentAttributes(validId, validData, writeContext);
        verifyWriteInvoked(true, VALID_NAME);
    }

    @Test
    public void writeCurrentAttributesFailed() {
        onWriteThrow();

        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            verifyWriteInvoked(true, VALID_NAME);
            return;
        }

        fail("Test should have thrown exception");
    }

    @Test
    public void updateCurrentAttributes() {
        try {
            customizer.updateCurrentAttributes(validId, validData, validData, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof UnsupportedOperationException);
            return;
        }

        fail("Test should have thrown exception");
    }

    @Test
    public void deleteCurrentAttributesSuccess() throws Exception {
        onWriteSuccess();
        customizer.deleteCurrentAttributes(validId, validData, writeContext);
        verifyWriteInvoked(false, VALID_NAME);
    }

    @Test
    public void deleteCurrentAttributesFailed() throws Exception {
        onWriteThrow();

        try {
            customizer.writeCurrentAttributes(validId, validData, writeContext);
        } catch (WriteFailedException e) {
            assertTrue(e.getCause() instanceof VppCallbackException);
            verifyWriteInvoked(true, VALID_NAME);
            return;
        }

        fail("Test should have thrown exception");
    }

    private void onWriteSuccess() {
        when(api.lispAddDelMapRequestItrRlocs(any(LispAddDelMapRequestItrRlocs.class)))
                .thenReturn(CompletableFuture.completedFuture(new LispAddDelMapRequestItrRlocsReply()));
    }

    private void onWriteThrow() {
        when(api.lispAddDelMapRequestItrRlocs(any(LispAddDelMapRequestItrRlocs.class)))
                .thenReturn(new CompletableFuture<LispAddDelMapRequestItrRlocsReply>() {
                    @Override
                    public LispAddDelMapRequestItrRlocsReply get(final long l, final TimeUnit timeUnit)
                            throws InterruptedException, ExecutionException, TimeoutException {
                        throw new ExecutionException(new VppCallbackException("lispAddDelMapRequestItrRlocs", 1, -2));
                    }
                });
    }

    private void verifyWriteInvoked(final boolean add, final String name) {
        verify(api, times(1)).lispAddDelMapRequestItrRlocs(requestCaptor.capture());

        final LispAddDelMapRequestItrRlocs request = requestCaptor.getValue();
        assertNotNull(request);
        assertEquals(booleanToByte(add), request.isAdd);
        assertEquals(name, toString(request.locatorSetName));
    }
}