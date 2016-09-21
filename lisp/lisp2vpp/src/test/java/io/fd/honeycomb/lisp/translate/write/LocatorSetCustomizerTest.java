/*
 * Copyright (c) 2015 Cisco and/or its affiliates.
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

package io.fd.honeycomb.lisp.translate.write;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.translate.write.WriteFailedException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.LocatorSets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.LocatorSetKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev160520.locator.sets.grouping.locator.sets.locator.set.InterfaceBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.dto.LispAddDelLocatorSet;
import org.openvpp.jvpp.core.dto.LispAddDelLocatorSetReply;
import org.openvpp.jvpp.core.dto.LispLocatorSetDetails;
import org.openvpp.jvpp.core.dto.LispLocatorSetDetailsReplyDump;
import org.openvpp.jvpp.core.future.FutureJVppCore;


public class LocatorSetCustomizerTest {

    private FutureJVppCore fakeJvpp;
    private NamingContext locatorSetContext;
    private MappingContext mappingContext;
    private ModificationCache cache;
    private WriteContext context;

    @Before
    public void init() {
        fakeJvpp = mock(FutureJVppCore.class);
        locatorSetContext = new NamingContext("locator-set", "instance");
        context = mock(WriteContext.class);
        mappingContext = mock(MappingContext.class);
        cache = new ModificationCache();
        when(context.getMappingContext()).thenReturn(mappingContext);
        when(context.getModificationCache()).thenReturn(cache);
    }


    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesNullData() throws WriteFailedException {
        new LocatorSetCustomizer(fakeJvpp, locatorSetContext).writeCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testWriteCurrentAttributesBadData() throws WriteFailedException {
        new LocatorSetCustomizer(fakeJvpp, locatorSetContext)
                .writeCurrentAttributes(null, mock(LocatorSet.class), null);
    }

    @Test
    public void testWriteCurrentAttributes() throws WriteFailedException, InterruptedException, ExecutionException {

        LocatorSet locatorSet = new LocatorSetBuilder()
                .setName("Locator")
                .setInterface(Arrays.asList(new InterfaceBuilder().build()))
                .build();

        InstanceIdentifier<LocatorSet> validId =
                InstanceIdentifier.create(LocatorSets.class).child(LocatorSet.class, new LocatorSetKey("Locator"));


        ArgumentCaptor<LispAddDelLocatorSet> locatorSetCaptor = ArgumentCaptor.forClass(LispAddDelLocatorSet.class);

        LispAddDelLocatorSetReply fakeReply = new LispAddDelLocatorSetReply();

        CompletableFuture<LispAddDelLocatorSetReply> completeFuture = new CompletableFuture<>();
        completeFuture.complete(fakeReply);

        when(fakeJvpp.lispAddDelLocatorSet(any(LispAddDelLocatorSet.class))).thenReturn(completeFuture);
        when(context.readAfter(validId)).thenReturn(Optional.of(locatorSet));

        final LispLocatorSetDetailsReplyDump reply = new LispLocatorSetDetailsReplyDump();
        LispLocatorSetDetails details = new LispLocatorSetDetails();
        details.lsName = "Locator".getBytes(StandardCharsets.UTF_8);
        reply.lispLocatorSetDetails = ImmutableList.of(details);

        cache.put(io.fd.honeycomb.lisp.translate.read.LocatorSetCustomizer.LOCATOR_SETS_CACHE_ID, reply);

        new LocatorSetCustomizer(fakeJvpp, locatorSetContext).writeCurrentAttributes(validId, locatorSet, context);

        verify(fakeJvpp, times(1)).lispAddDelLocatorSet(locatorSetCaptor.capture());

        LispAddDelLocatorSet request = locatorSetCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals(1, request.isAdd);

    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUpdateCurrentAttributes() throws WriteFailedException {
        new LocatorSetCustomizer(fakeJvpp, locatorSetContext).updateCurrentAttributes(null, null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesNullData() throws WriteFailedException {
        new LocatorSetCustomizer(fakeJvpp, locatorSetContext).deleteCurrentAttributes(null, null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testDeleteCurrentAttributesBadData() throws WriteFailedException {
        new LocatorSetCustomizer(fakeJvpp, locatorSetContext)
                .deleteCurrentAttributes(null, mock(LocatorSet.class), null);
    }

    @Test
    public void testDeleteCurrentAttributes() throws InterruptedException, ExecutionException, WriteFailedException {

        LocatorSet locatorSet = new LocatorSetBuilder()
                .setName("Locator")
                .build();

        ArgumentCaptor<LispAddDelLocatorSet> locatorSetCaptor = ArgumentCaptor.forClass(LispAddDelLocatorSet.class);

        LispAddDelLocatorSetReply fakeReply = new LispAddDelLocatorSetReply();

        CompletableFuture<LispAddDelLocatorSetReply> completeFuture = new CompletableFuture<>();
        completeFuture.complete(fakeReply);

        when(fakeJvpp.lispAddDelLocatorSet(any(LispAddDelLocatorSet.class))).thenReturn(completeFuture);

        new LocatorSetCustomizer(fakeJvpp, locatorSetContext).deleteCurrentAttributes(null, locatorSet, context);

        verify(fakeJvpp, times(1)).lispAddDelLocatorSet(locatorSetCaptor.capture());

        LispAddDelLocatorSet request = locatorSetCaptor.getValue();

        assertNotNull(request);
        assertEquals("Locator", new String(request.locatorSetName));
        assertEquals(0, request.isAdd);

    }
}
