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

package io.fd.hc2vpp.acl.write;

import io.fd.hc2vpp.acl.AclTestSchemaContext;
import io.fd.hc2vpp.common.test.write.WriterCustomizerTest;
import io.fd.vpp.jvpp.acl.future.FutureJVppAclFacade;
import org.junit.Test;
import org.mockito.Mock;

public class InterfaceAclCustomizerTest extends WriterCustomizerTest implements AclTestSchemaContext {

    @Mock
    private FutureJVppAclFacade aclApi;

    @Override
    protected void setUpTest() throws Exception {

    }

    @Test
    public void writeCurrentAttributes() throws Exception {

    }

    @Test
    public void updateCurrentAttributes() throws Exception {

    }

    @Test
    public void deleteCurrentAttributes() throws Exception {

    }

}