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

package io.fd.honeycomb.v3po.vpp.data.init;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class InitializerRegistryImplTest {

    @Mock(name="dti1")
    private DataTreeInitializer dti1;
    @Mock(name="dti2")
    private DataTreeInitializer dti2;
    @Mock(name="dti3")
    private DataTreeInitializer dti3;

    private ArrayList<DataTreeInitializer> initializers;

    private InitializerRegistryImpl initializerRegistry;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        initializers = new ArrayList<>();
        initializers.add(dti1);
        initializers.add(dti2);
        initializers.add(dti3);
        initializerRegistry = new InitializerRegistryImpl(initializers);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFailed() throws Exception {
        new InitializerRegistryImpl(Arrays.asList(dti1, null));
    }

    @Test
    public void testInitialize() throws Exception {
        initializerRegistry.initialize();

        verify(dti1).initialize();
        verify(dti2).initialize();
        verify(dti3).initialize();
    }

    @Test
    public void testClose() throws Exception {
        initializerRegistry.close();

        verify(dti1).close();
        verify(dti2).close();
        verify(dti3).close();
    }
}