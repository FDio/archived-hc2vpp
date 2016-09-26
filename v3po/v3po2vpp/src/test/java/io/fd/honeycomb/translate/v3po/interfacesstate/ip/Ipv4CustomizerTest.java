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

package io.fd.honeycomb.translate.v3po.interfacesstate.ip;

import static org.mockito.Mockito.verifyZeroInteractions;

import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.honeycomb.vpp.test.read.ReaderCustomizerTest;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.Interface2Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ip.rev140616.interfaces.state._interface.Ipv4Builder;

public class Ipv4CustomizerTest extends ReaderCustomizerTest<Ipv4, Ipv4Builder> {

    public Ipv4CustomizerTest() {
        super(Ipv4.class, Interface2Builder.class);
    }

    @Test
    public void testReadCurrentAttributes() throws Exception {
        customizer.readCurrentAttributes(null, null, ctx);
        verifyZeroInteractions(api);
    }

    @Override
    protected ReaderCustomizer<Ipv4, Ipv4Builder> initCustomizer() {
        return new Ipv4Customizer(api);
    }
}