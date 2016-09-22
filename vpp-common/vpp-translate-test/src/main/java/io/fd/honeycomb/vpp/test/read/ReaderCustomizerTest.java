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

package io.fd.honeycomb.vpp.test.read;

import static org.junit.Assert.assertNotNull;

import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.ModificationCache;
import io.fd.honeycomb.translate.read.ReadContext;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.core.future.FutureJVppCore;

/**
 * Generic test for classes implementing {@link ReaderCustomizer} interface.
 *
 * @param <D> Specific DataObject derived type (Identifiable), that is handled by this customizer
 * @param <B> Specific Builder for handled type (D)
 */
public abstract class ReaderCustomizerTest<D extends DataObject, B extends Builder<D>> {

    @Mock
    protected FutureJVppCore api;
    @Mock
    protected ReadContext ctx;
    @Mock
    protected MappingContext mappingContext;

    protected ModificationCache cache;
    protected final Class<D> dataObjectClass;
    private ReaderCustomizer<D, B> customizer;

    protected ReaderCustomizerTest(Class<D> dataObjectClass) {
        this.dataObjectClass = dataObjectClass;
    }

    @Before
    public void setUpParent() throws Exception {
        MockitoAnnotations.initMocks(this);
        cache = new ModificationCache();
        Mockito.doReturn(cache).when(ctx).getModificationCache();
        Mockito.doReturn(mappingContext).when(ctx).getMappingContext();
        setUp();
        customizer = initCustomizer();
    }

    /**
     * Optional setup for subclasses. Invoked before customizer is initialized.
     */
    protected void setUp() throws Exception {
    }

    protected abstract ReaderCustomizer<D, B> initCustomizer();

    protected ReaderCustomizer<D, B> getCustomizer() {
        return customizer;
    }

    @Test
    public void testGetBuilder() throws Exception {
        assertNotNull(customizer.getBuilder(InstanceIdentifier.create(dataObjectClass)));
    }

    // TODO HONEYCOMB-116: create generic testMerge()
}
