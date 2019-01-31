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

package io.fd.hc2vpp.vpp.classifier.write.acl;

import static org.mockito.MockitoAnnotations.initMocks;

import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.hc2vpp.vpp.classifier.context.VppClassifierContextManager;
import io.fd.hc2vpp.vpp.classifier.write.acl.ingress.AclValidator;
import io.fd.honeycomb.translate.write.DataValidationFailedException.CreateValidationFailedException;
import io.fd.honeycomb.translate.write.WriteContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip4Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip4AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip6Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.Ip6AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.L2Acl;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.acl.base.attributes.L2AclBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.Ingress;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.classifier.acl.rev170503.vpp.acl.attributes.acl.IngressBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class AclValidatorTest {

    @Mock
    private VppClassifierContextManager classifyTableContext;
    @Mock
    private WriteContext writeContext;

    private AclValidator validator;
    private NamingContext interfaceContext;

    private InstanceIdentifier<Ingress> ingressIID;
    private static final String ACL_TABLE_NAME = "table0";

    @Before
    public void setUp() {
        initMocks(this);
        ingressIID = InstanceIdentifier.create(Ingress.class);
        interfaceContext = new NamingContext("testAclValidator", "testAclValidator");
        validator = new AclValidator(interfaceContext, classifyTableContext);
    }

    @Test
    public void testWriteSuccessfullL24Acl()
            throws CreateValidationFailedException {
        final IngressBuilder builder = new IngressBuilder();
        L2Acl l2Acl = new L2AclBuilder().setClassifyTable(ACL_TABLE_NAME).build();
        builder.setL2Acl(l2Acl);
        validator.validateWrite(ingressIID, builder.build(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedL2Acl()
            throws CreateValidationFailedException {
        final IngressBuilder builder = new IngressBuilder();
        L2Acl l2Acl = new L2AclBuilder().setClassifyTable(null).build();
        builder.setL2Acl(l2Acl);
        validator.validateWrite(ingressIID, builder.build(), writeContext);
    }

    @Test
    public void testWriteSuccessfullIp4Acl()
            throws CreateValidationFailedException {
        final IngressBuilder builder = new IngressBuilder();
        Ip4Acl ip42Acl = new Ip4AclBuilder().setClassifyTable(ACL_TABLE_NAME).build();
        builder.setIp4Acl(ip42Acl);
        validator.validateWrite(ingressIID, builder.build(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedIp4Acl()
            throws CreateValidationFailedException {
        final IngressBuilder builder = new IngressBuilder();
        Ip4Acl ip42Acl = new Ip4AclBuilder().setClassifyTable(null).build();
        builder.setIp4Acl(ip42Acl);
        validator.validateWrite(ingressIID, builder.build(), writeContext);
    }

    @Test
    public void testWriteSuccessfullIp6Acl()
            throws CreateValidationFailedException {
        final IngressBuilder builder = new IngressBuilder();
        Ip6Acl ip6Acl = new Ip6AclBuilder().setClassifyTable(ACL_TABLE_NAME).build();
        builder.setIp6Acl(ip6Acl);
        validator.validateWrite(ingressIID, builder.build(), writeContext);
    }

    @Test(expected = CreateValidationFailedException.class)
    public void testWriteFailedIp6Acl()
            throws CreateValidationFailedException {
        final IngressBuilder builder = new IngressBuilder();
        Ip6Acl ip6Acl = new Ip6AclBuilder().setClassifyTable(null).build();
        builder.setIp6Acl(ip6Acl);
        validator.validateWrite(ingressIID, builder.build(), writeContext);
    }
}
