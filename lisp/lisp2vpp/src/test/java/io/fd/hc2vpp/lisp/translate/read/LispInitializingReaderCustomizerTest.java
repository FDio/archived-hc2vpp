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

package io.fd.hc2vpp.lisp.translate.read;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.read.InitializingReaderCustomizerTest;
import io.fd.hc2vpp.lisp.translate.service.LispStateCheckService;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.read.ReadFailedException;
import java.util.Objects;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public abstract class LispInitializingReaderCustomizerTest<D extends DataObject, B extends Builder<D>> extends
        InitializingReaderCustomizerTest<D, B> {

    @Mock
    protected LispStateCheckService lispStateCheckService;

    public LispInitializingReaderCustomizerTest(final Class<D> dataObjectClass,
                                                final Class<? extends Builder<? extends DataObject>> parentBuilderClass) {
        super(dataObjectClass, parentBuilderClass);
    }

    protected void mockLispEnabled() {
        when(lispStateCheckService.lispEnabled(any(ReadContext.class))).thenReturn(true);
    }

    @Test
    public void testNoInteractionsWhileLispDisabled() throws ReadFailedException {
        when(lispStateCheckService.lispEnabled(any(ReadContext.class))).thenReturn(false);
        final InstanceIdentifier<D> identifier = InstanceIdentifier.create(dataObjectClass);
        final B builderTouched = getCustomizer().getBuilder(identifier);
        final B builderUntouched = getCustomizer().getBuilder(identifier);
        getCustomizer().readCurrentAttributes(identifier, builderTouched, ctx);
        assertTrue("No interactions with builder expected while lisp is disabled",
                Objects.equals(builderTouched.build(), builderUntouched.build()));
    }
}
