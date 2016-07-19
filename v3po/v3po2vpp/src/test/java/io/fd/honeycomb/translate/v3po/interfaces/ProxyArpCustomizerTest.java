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

package io.fd.honeycomb.translate.v3po.interfaces;

import io.fd.honeycomb.translate.v3po.interfaces.ProxyArpCustomizer;
import io.fd.honeycomb.translate.MappingContext;
import io.fd.honeycomb.translate.v3po.util.NamingContext;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.openvpp.jvpp.future.FutureJVpp;

import static org.mockito.Mockito.doReturn;

public class ProxyArpCustomizerTest {

    @Mock
    private FutureJVpp vppApi;
    @Mock
    private WriteContext writeContext;
    @Mock
    private MappingContext mappingContext;

    private ProxyArpCustomizer proxyArpCustomizer;
    private NamingContext namingContext;

    @Before
    protected void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        namingContext = new NamingContext("generatedSubInterfaceName", "test-instance");
        doReturn(mappingContext).when(writeContext).getMappingContext();

        proxyArpCustomizer = new ProxyArpCustomizer(vppApi, namingContext);
    }
}
