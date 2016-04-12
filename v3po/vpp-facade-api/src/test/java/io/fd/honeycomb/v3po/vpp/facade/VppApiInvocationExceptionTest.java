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

package io.fd.honeycomb.v3po.vpp.facade;

import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class VppApiInvocationExceptionTest {

    @Test
    public void testInstantiation() {
        final String apiMethodName = "methodName";
        final int ctxId = 1;
        final int code = -1;
        VppApiInvocationException e = new VppApiInvocationException(apiMethodName, ctxId, code);
        Assert.assertEquals(apiMethodName, e.getMethodName());
        Assert.assertEquals(ctxId, e.getCtxId());
        Assert.assertEquals(code, e.getErrorCode());
        Assert.assertTrue(e.getMessage().contains(apiMethodName));
        Assert.assertTrue(e.getMessage().contains(String.valueOf(code)));
        Assert.assertTrue(e.getMessage().contains(String.valueOf(ctxId)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstantiationFailed() {
        final int code = new Random().nextInt(Integer.MAX_VALUE);
        VppApiInvocationException e = new VppApiInvocationException("apiMethodName", 1, code);
    }
}
