/*
 * Copyright (c) 2017 Cisco and/or its affiliates.
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

package io.fd.hc2vpp.lisp.translate.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.base.Optional;
import io.fd.hc2vpp.common.test.util.FutureProducer;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.vpp.jvpp.core.dto.ShowLispStatusReply;
import io.fd.vpp.jvpp.core.future.FutureJVppCore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.LispBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LispStateCheckServiceImplTest implements FutureProducer {

    private LispStateCheckService impl;

    @Mock
    private FutureJVppCore vppApi;

    @Mock
    private WriteContext writeContext;

    @Mock
    private ReadContext readContext;

    @Before
    public void init() {
        initMocks(this);
        impl = new LispStateCheckServiceImpl(vppApi);
        when(readContext.getModificationCache()).thenReturn(new ModificationCache());
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckLispEnabledNoConfig() throws Exception {
        when(writeContext.readAfter(InstanceIdentifier.create(Lisp.class))).thenReturn(Optional.absent());
        impl.checkLispEnabled(writeContext);
    }

    @Test(expected = IllegalStateException.class)
    public void testCheckLispEnabledDisabledConfig() throws Exception {
        when(writeContext.readAfter(InstanceIdentifier.create(Lisp.class)))
                .thenReturn(Optional.of(new LispBuilder().setEnable(false).build()));
        impl.checkLispEnabled(writeContext);
    }

    @Test
    public void testCheckLispEnabledEnabledConfig() throws Exception {
        // no exception should be thrown here
        when(writeContext.readAfter(InstanceIdentifier.create(Lisp.class)))
                .thenReturn(Optional.of(new LispBuilder().setEnable(true).build()));
        impl.checkLispEnabled(writeContext);
    }

    @Test
    public void testLispEnabledDisabledDump() throws Exception {
        when(vppApi.showLispStatus(any())).thenReturn(future(new ShowLispStatusReply()));
        assertFalse(impl.lispEnabled(readContext));
    }

    @Test
    public void testLispEnabledEnabledDump() throws Exception {
        final ShowLispStatusReply reply = new ShowLispStatusReply();
        reply.featureStatus = 1;
        when(vppApi.showLispStatus(any())).thenReturn(future(reply));
        assertTrue(impl.lispEnabled(readContext));
    }
}