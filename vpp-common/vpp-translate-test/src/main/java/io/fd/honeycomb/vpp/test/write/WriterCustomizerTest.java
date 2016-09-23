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

package io.fd.honeycomb.vpp.test.write;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.spi.write.WriterCustomizer;
import io.fd.honeycomb.translate.write.WriteContext;
import io.fd.honeycomb.vpp.test.util.FutureProducer;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openvpp.jvpp.core.future.FutureJVppCore;

/**
 * Generic test for classes implementing {@link WriterCustomizer} interface.
 */
public abstract class WriterCustomizerTest implements FutureProducer {

    @Mock
    protected FutureJVppCore api;
    @Mock
    protected WriteContext writeContext;
    @Mock
    protected MappingContext mappingContext;

    protected ModificationCache cache;

    protected WriterCustomizerTest() {
    }

    @Before
    public void setUpParent() throws Exception {
        MockitoAnnotations.initMocks(this);
        cache = new ModificationCache();
        Mockito.doReturn(cache).when(writeContext).getModificationCache();
        Mockito.doReturn(mappingContext).when(writeContext).getMappingContext();
        setUp();
    }

    /**
     * Optional setup for subclasses. Invoked after parent initialization.
     */
    protected void setUp() throws Exception {
    }
}
