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

package io.fd.hc2vpp.lisp.translate.read;

import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.test.read.InitializingReaderCustomizerTest;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.ShowLispStatusReply;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170315.LispStateBuilder;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(HoneycombTestRunner.class)
public class LispStateCustomizerTest extends InitializingReaderCustomizerTest<LispState, LispStateBuilder>
    implements InjectablesProcessor{

    private InstanceIdentifier<LispState> identifier;

    public LispStateCustomizerTest() {
        super(LispState.class, null);
    }

    @Before
    public void init() {
        identifier = InstanceIdentifier.create(LispState.class);
        final ShowLispStatusReply reply = new ShowLispStatusReply();
        reply.featureStatus = 1;

        when(api.showLispStatus(Mockito.any())).thenReturn(future(reply));
    }

    @Test
    public void testReadCurrentAttributes() throws ReadFailedException {

        LispStateBuilder builder = new LispStateBuilder();
        getCustomizer().readCurrentAttributes(identifier, builder, ctx);

        assertEquals(true, builder.build().isEnable());
    }

    @SchemaContextProvider
    public ModuleInfoBackedContext schemaContext() {
        return provideSchemaContextFor(ImmutableSet.of($YangModuleInfoImpl.getInstance()));
    }


    @Test
    public void testInit(@InjectTestData(resourcePath = "/lisp-config.json") Lisp config,
                         @InjectTestData(resourcePath = "/lisp-operational.json") LispState operational) {
        final InstanceIdentifier<LispState> operationalPath = InstanceIdentifier.create(LispState.class);
        final InstanceIdentifier<Lisp> configPath = InstanceIdentifier.create(Lisp.class);

        invokeInitTest(operationalPath, operational, configPath, config);
    }

    @Override
    protected ReaderCustomizer<LispState, LispStateBuilder> initCustomizer() {
        return new LispStateCustomizer(api);
    }

    @Override
    public void testMerge() throws Exception {
        //LispState is root node, so there is no way to implement merge(it is also not needed)
    }
}
