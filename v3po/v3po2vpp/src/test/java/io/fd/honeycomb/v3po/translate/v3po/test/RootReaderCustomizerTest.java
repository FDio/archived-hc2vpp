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

package io.fd.honeycomb.v3po.translate.v3po.test;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.honeycomb.v3po.translate.MappingContext;
import io.fd.honeycomb.v3po.translate.ModificationCache;
import io.fd.honeycomb.v3po.translate.read.ReadContext;
import io.fd.honeycomb.v3po.translate.spi.read.RootReaderCustomizer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yangtools.concepts.Builder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.openvpp.jvpp.future.FutureJVpp;

/**
 * Generic test for classes implementing {@link RootReaderCustomizer} interface.
 *
 * @param <D> Specific DataObject derived type (Identifiable), that is handled by this customizer
 * @param <B> Specific Builder for handled type (D)
 */
public abstract class RootReaderCustomizerTest<D extends DataObject, B extends Builder<D>> {

    @Mock
    protected FutureJVpp api;
    protected ModificationCache cache;
    @Mock
    protected ReadContext ctx;
    @Mock
    protected MappingContext mappingContext;

    protected final Class<D> dataObjectClass;
    private RootReaderCustomizer<D, B> customizer;

    protected RootReaderCustomizerTest(Class<D> dataObjectClass) {
        this.dataObjectClass = dataObjectClass;
    }

    @Before
    public void setUpParent() throws Exception {
        initMocks(this);
        cache = new ModificationCache();
        doReturn(cache).when(ctx).getModificationCache();
        doReturn(mappingContext).when(ctx).getMappingContext();

        setUpBefore();
        customizer = initCustomizer();
        setUpAfter();
    }

    /**
     * Optional setup for subclasses. Invoked before customizer is initialized.
     */
    protected void setUpBefore() {

    }

    /**
     * Optional setup for subclasses. Invoked after customizer is initialized.
     */
    protected void setUpAfter() throws Exception {

    }

    protected abstract RootReaderCustomizer<D, B> initCustomizer();

    protected RootReaderCustomizer<D, B> getCustomizer() {
        return customizer;
    }

    @Test
    public void testGetBuilder() throws Exception {
        assertNotNull(customizer.getBuilder(InstanceIdentifier.create(dataObjectClass)));
    }
}
