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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import io.fd.hc2vpp.common.test.read.InitializingReaderCustomizerTest;
import io.fd.hc2vpp.common.test.util.NamingContextHelper;
import io.fd.hc2vpp.common.translate.util.NamingContext;
import io.fd.honeycomb.test.tools.HoneycombTestRunner;
import io.fd.honeycomb.test.tools.annotations.InjectTestData;
import io.fd.honeycomb.test.tools.annotations.InjectablesProcessor;
import io.fd.honeycomb.test.tools.annotations.SchemaContextProvider;
import io.fd.honeycomb.translate.read.ReadFailedException;
import io.fd.honeycomb.translate.spi.read.ReaderCustomizer;
import io.fd.vpp.jvpp.core.dto.OneLocatorSetDetails;
import io.fd.vpp.jvpp.core.dto.OneLocatorSetDetailsReplyDump;
import io.fd.vpp.jvpp.core.dto.ShowOneStatusReply;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.Contexts;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContextKey;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.Mappings;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.MappingsBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.Mapping;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingBuilder;
import org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.naming.context.mappings.MappingKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.$YangModuleInfoImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.Lisp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.LispState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.lisp.rev170911.LispStateBuilder;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

@RunWith(HoneycombTestRunner.class)
public class LispStateCustomizerTest extends InitializingReaderCustomizerTest<LispState, LispStateBuilder>
        implements InjectablesProcessor, NamingContextHelper {


    private NamingContext locatorSetContext;

    private InstanceIdentifier<LispState> identifier;

    public LispStateCustomizerTest() {
        super(LispState.class, null);
    }

    @Override
    @Before
    public void setUp() {
        identifier = InstanceIdentifier.create(LispState.class);
        final ShowOneStatusReply reply = new ShowOneStatusReply();
        reply.featureStatus = 1;

        when(api.showOneStatus(Mockito.any())).thenReturn(future(reply));
        locatorSetContext = new NamingContext("loc-set", "locator-set-context");
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

        when(api.oneLocatorSetDump(any())).thenReturn(future(new OneLocatorSetDetailsReplyDump()));

        invokeInitTest(operationalPath, operational, configPath, config);
    }

    @Test
    public void testInitWithLocatorSetContextInit(@InjectTestData(resourcePath = "/lisp-config.json") Lisp config,
                                                  @InjectTestData(resourcePath = "/lisp-operational.json") LispState operational) {
        mockLocatorSetDump();
        final InstanceIdentifier<LispState> operationalPath = InstanceIdentifier.create(LispState.class);
        final InstanceIdentifier<Lisp> configPath = InstanceIdentifier.create(Lisp.class);

        final KeyedInstanceIdentifier<org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext, NamingContextKey>
                namingContextId = InstanceIdentifier.create(Contexts.class).child(
                org.opendaylight.yang.gen.v1.urn.honeycomb.params.xml.ns.yang.naming.context.rev160513.contexts.NamingContext.class,
                new NamingContextKey("locator-set-context"));
        final KeyedInstanceIdentifier<Mapping, MappingKey> loc1Key = namingContextId
                .child(Mappings.class).child(Mapping.class, new MappingKey("loc_1"));

        final Mapping loc1Data = new MappingBuilder().setIndex(0).setName("loc_1").build();

        final KeyedInstanceIdentifier<Mapping, MappingKey> loc2Key = namingContextId
                .child(Mappings.class).child(Mapping.class, new MappingKey("loc_2"));

        final Mapping loc2Data = new MappingBuilder().setIndex(1).setName("loc_2").build();

        when(mappingContext.read(namingContextId.child(Mappings.class)))
                .thenReturn(Optional.of(new MappingsBuilder().setMapping(Arrays.asList(loc1Data, loc2Data)).build()));
        when(mappingContext.read(loc1Key)).thenReturn(Optional.absent())
                .thenReturn(Optional.of(loc1Data)).thenReturn(Optional.of(loc1Data));
        when(mappingContext.read(loc2Key)).thenReturn(Optional.absent())
                .thenReturn(Optional.of(loc2Data)).thenReturn(Optional.of(loc2Data));


        invokeInitTest(operationalPath, operational, configPath, config);

        // first read is inside contains,second one is for logger,and its x 2 locator sets
        verify(mappingContext, times(4)).read(namingContextId.child(Mappings.class));
        verify(mappingContext, times(1)).put(loc1Key, loc1Data);
        verify(mappingContext, times(1)).read(loc1Key);
        verify(mappingContext, times(1)).put(loc2Key, loc2Data);
        verify(mappingContext, times(1)).read(loc2Key);
        verifyNoMoreInteractions(mappingContext);
    }

    private void mockLocatorSetDump() {
        OneLocatorSetDetailsReplyDump replyDump = new OneLocatorSetDetailsReplyDump();
        OneLocatorSetDetails locator1 = new OneLocatorSetDetails();
        locator1.lsIndex = 0;
        locator1.lsName = "loc_1".getBytes(StandardCharsets.UTF_8);
        OneLocatorSetDetails locator2 = new OneLocatorSetDetails();
        locator2.lsIndex = 1;
        locator2.lsName = "loc_2".getBytes(StandardCharsets.UTF_8);

        replyDump.oneLocatorSetDetails = Arrays.asList(locator1, locator2);

        when(api.oneLocatorSetDump(any())).thenReturn(future(replyDump));
    }

    @Override
    protected ReaderCustomizer<LispState, LispStateBuilder> initCustomizer() {
        return new LispStateCustomizer(api, locatorSetContext);
    }

    @Override
    public void testMerge() throws Exception {
        //LispState is root node, so there is no way to implement merge(it is also not needed)
    }
}
