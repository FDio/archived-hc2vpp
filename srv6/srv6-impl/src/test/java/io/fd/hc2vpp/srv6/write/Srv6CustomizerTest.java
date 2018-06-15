/*
 * Copyright (c) 2018 Bell Canada, Pantheon Technologies and/or its affiliates.
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


package io.fd.hc2vpp.srv6.write;

import io.fd.hc2vpp.srv6.Srv6IIds;
import io.fd.hc2vpp.srv6.write.sid.request.LocalSidRequestTest;
import io.fd.honeycomb.translate.write.WriteFailedException;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.routing.Srv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.srv6.base.rev180301.routing.Srv6Builder;

public class Srv6CustomizerTest extends LocalSidRequestTest {

    @Test(expected = WriteFailedException.class)
    public void writeCurrentAttributesNullTest() throws WriteFailedException {
        Srv6Customizer customizer = new Srv6Customizer();
        Srv6 srv6 = new Srv6Builder().setEnable(true).build();
        customizer.writeCurrentAttributes(Srv6IIds.RT_SRV6, srv6, ctx);
    }

    @Test(expected = WriteFailedException.class)
    public void deleteCurrentAttributesNullTest() throws WriteFailedException {
        Srv6Customizer customizer = new Srv6Customizer();
        Srv6 srv6 = new Srv6Builder().setEnable(true).build();
        customizer.deleteCurrentAttributes(Srv6IIds.RT_SRV6, srv6, ctx);
    }
}
