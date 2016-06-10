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
package io.fd.honeycomb.v3po.translate.v3po.vppstate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.fd.honeycomb.v3po.translate.spi.read.RootReaderCustomizer;
import io.fd.honeycomb.v3po.translate.v3po.test.ListReaderCustomizerTest;
import io.fd.honeycomb.v3po.translate.v3po.util.NamingContext;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.BridgeDomainsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.vpp.state.bridge.domains.BridgeDomainKey;

public class BridgeDomainCustomizerTest extends ListReaderCustomizerTest<BridgeDomain, BridgeDomainKey, BridgeDomainBuilder> {

    private NamingContext bdContext;
    private NamingContext interfacesContext;

    public BridgeDomainCustomizerTest() {
        super(BridgeDomain.class);
    }

    @Override
    public void setUpBefore() {
        bdContext = new NamingContext("generatedBdName", "bd-test-instance");
        interfacesContext = new NamingContext("generatedIfaceName", "ifc-test-instance");
    }

    @Test
    public void testMerge() throws Exception {
        final BridgeDomainsBuilder builder = mock(BridgeDomainsBuilder.class);
        final List<BridgeDomain> value = Collections.emptyList();
        getCustomizer().merge(builder, value);
        verify(builder).setBridgeDomain(value);
    }

    @Override
    protected RootReaderCustomizer<BridgeDomain, BridgeDomainBuilder> initCustomizer() {
        return new BridgeDomainCustomizer(api, bdContext);
    }
}