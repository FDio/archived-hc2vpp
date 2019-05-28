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

package io.fd.hc2vpp.v3po.read.pbb;

import io.fd.hc2vpp.common.test.read.ReaderCustomizerTest;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.PbbRewriteInterfaceAugmentationBuilder;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.interfaces._interface.PbbRewrite;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.vpp.pbb.rev190527.interfaces._interface.PbbRewriteBuilder;

public class PbbRewriteCustomizerTest extends ReaderCustomizerTest<PbbRewrite, PbbRewriteBuilder> {

    public PbbRewriteCustomizerTest() {
        super(PbbRewrite.class, PbbRewriteInterfaceAugmentationBuilder.class);
    }

    @Override
    protected ReaderCustomizer<PbbRewrite, PbbRewriteBuilder> initCustomizer() {
        return new PbbRewriteCustomizer(api);
    }
}
