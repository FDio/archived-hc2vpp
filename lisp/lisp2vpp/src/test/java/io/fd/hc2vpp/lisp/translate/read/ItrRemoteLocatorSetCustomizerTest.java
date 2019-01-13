/*
 * Copyright (c) 2016 Cisco and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fd.hc2vpp.lisp.translate.read;

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
import io.fd.vpp.jvpp.VppCallbackException;
import io.fd.vpp.jvpp.core.dto.OneGetMapRequestItrRlocs;
import io.fd.vpp.jvpp.core.dto.OneGetMapRequestItrRlocsReply;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.itr.remote.locator.sets.grouping.ItrRemoteLocatorSet;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.itr.remote.locator.sets.grouping.ItrRemoteLocatorSetBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.lisp.rev171013.lisp.feature.data.grouping.LispFeatureDataBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class ItrRemoteLocatorSetCustomizerTest
        extends LispInitializingReaderCustomizerTest<ItrRemoteLocatorSet, ItrRemoteLocatorSetBuilder> {

    private static final String EXPECTED_LOCATOR_SET_NAME = "loc-set";

    private InstanceIdentifier<ItrRemoteLocatorSet> validId;
    private ItrRemoteLocatorSetBuilder builder;

    public ItrRemoteLocatorSetCustomizerTest() {
        super(ItrRemoteLocatorSet.class, LispFeatureDataBuilder.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        validId = InstanceIdentifier.create(ItrRemoteLocatorSet.class);
        builder = new ItrRemoteLocatorSetBuilder();
        mockLispEnabled();
    }

    @Override
    protected ReaderCustomizer<ItrRemoteLocatorSet, ItrRemoteLocatorSetBuilder> initCustomizer() {
        return new ItrRemoteLocatorSetCustomizer(api, lispStateCheckService);
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
        verifyOneGetMapRequestItrRlocsInvokedOnce();
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

            verifyOneGetMapRequestItrRlocsInvokedOnce();
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
        OneGetMapRequestItrRlocsReply reply = new OneGetMapRequestItrRlocsReply();
        reply.locatorSetName = EXPECTED_LOCATOR_SET_NAME.getBytes(StandardCharsets.UTF_8);

        when(api.oneGetMapRequestItrRlocs(any(OneGetMapRequestItrRlocs.class)))
                .thenReturn(CompletableFuture.completedFuture(reply));
    }

    private void doReturnNullDataOnDump() {
        when(api.oneGetMapRequestItrRlocs(any(OneGetMapRequestItrRlocs.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    private void doReturnEmptyDataOnDump() {
        when(api.oneGetMapRequestItrRlocs(any(OneGetMapRequestItrRlocs.class)))
                .thenReturn(CompletableFuture.completedFuture(new OneGetMapRequestItrRlocsReply()));
    }

    private void doThrowExceptionOnDump() {
        when(api.oneGetMapRequestItrRlocs(any(OneGetMapRequestItrRlocs.class))).
                thenReturn(new CompletableFuture<OneGetMapRequestItrRlocsReply>() {
                    @Override
                    public OneGetMapRequestItrRlocsReply get(final long l, final TimeUnit timeUnit)
                            throws InterruptedException, ExecutionException, TimeoutException {
                        throw new ExecutionException(
                                new VppCallbackException("oneGetMapRequestItrRlocs", "oneGetMapRequestItrRlocs failed",
                                        1, -2));
                    }
                });
    }

    private void verifyOneGetMapRequestItrRlocsInvokedOnce() {
        verify(api, times(1)).oneGetMapRequestItrRlocs(any(OneGetMapRequestItrRlocs.class));
    }

    private void verifyInvalidDataCase(final ItrRemoteLocatorSetBuilder builder) {
        assertNotNull(builder);
        assertNull(builder.getRemoteLocatorSetName());

        verifyOneGetMapRequestItrRlocsInvokedOnce();
    }
}
