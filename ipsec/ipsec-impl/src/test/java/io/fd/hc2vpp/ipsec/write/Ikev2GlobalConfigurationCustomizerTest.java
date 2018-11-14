/*
 * Copyright (c) 2019 PANTHEON.tech.
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

package io.fd.hc2vpp.ipsec.write;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.hc2vpp.ipsec.helpers.SchemaContextTestHelper;
import io.fd.honeycomb.translate.write.WriteFailedException;
import io.fd.vpp.jvpp.core.dto.Ikev2SetLocalKey;
import io.fd.vpp.jvpp.core.dto.Ikev2SetLocalKeyReply;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecIkeGlobalConfAugmentation;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.ipsec.rev181213.IpsecIkeGlobalConfAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.Ikev2;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.IkeGlobalConfiguration;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.ipsec.rev181214.ikev2.IkeGlobalConfigurationBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class Ikev2GlobalConfigurationCustomizerTest extends WriterCustomizerTest
        implements SchemaContextTestHelper {
    InstanceIdentifier<IkeGlobalConfiguration> IID = InstanceIdentifier.create(Ikev2.class)
            .child(IkeGlobalConfiguration.class);
    private Ikev2GlobalConfigurationCustomizer customizer;
    private static final String LOCAL_KEY_FILE = "/home/localadmin/certs/client-key.pem";

    @Override
    protected void setUpTest() throws Exception {
        customizer = new Ikev2GlobalConfigurationCustomizer(api);
        when(api.ikev2SetLocalKey(any())).thenReturn(future(new Ikev2SetLocalKeyReply()));
    }

    @Test
    public void testWrite() throws WriteFailedException {
        IkeGlobalConfigurationBuilder dataAfterBuilder = new IkeGlobalConfigurationBuilder();
        IpsecIkeGlobalConfAugmentationBuilder augBuilder = new IpsecIkeGlobalConfAugmentationBuilder();
        augBuilder.setLocalKeyFile(LOCAL_KEY_FILE);
        dataAfterBuilder.addAugmentation(IpsecIkeGlobalConfAugmentation.class, augBuilder.build());
        customizer.writeCurrentAttributes(IID, dataAfterBuilder.build(), writeContext);
        Ikev2SetLocalKey request = new Ikev2SetLocalKey();
        request.keyFile = LOCAL_KEY_FILE.getBytes();
        verify(api).ikev2SetLocalKey(request);
    }
}
