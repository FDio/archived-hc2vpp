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


package io.fd.hc2vpp.srv6.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import io.fd.hc2vpp.srv6.write.sid.request.LocalSidRequestTest;
import io.fd.honeycomb.translate.MappingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.locator.context.rev180605.srv6.locator.context.attributes.srv6.locator.mappings.Srv6LocatorMapping;
import org.opendaylight.yang.gen.v1.http.fd.io.hc2vpp.yang.locator.context.rev180605.srv6.locator.context.attributes.srv6.locator.mappings.Srv6LocatorMappingBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class LocatorContextManagerImplTest extends LocalSidRequestTest {

    private static final Ipv6Prefix LOCATOR_PREFIX = new Ipv6Prefix("a::/64");

    @Mock
    MappingContext mappingContext;

    @Captor
    ArgumentCaptor<Srv6LocatorMapping> locMappingCaptor;

    @Captor
    ArgumentCaptor<InstanceIdentifier<Srv6LocatorMapping>> iidCaptor;

    @Before
    public void setup() {
        Srv6LocatorMapping locatorMapping =
                new Srv6LocatorMappingBuilder().setName(LOCATOR.getName()).setPrefix(LOCATOR_PREFIX).build();
        when(ctx.getMappingContext()).thenReturn(mappingContext);
        when(mappingContext.read(any())).thenReturn(Optional.of(locatorMapping));
    }

    @Test
    public void addLocatorTest() {
        LocatorContextManagerImpl contextManager = new LocatorContextManagerImpl(64);
        contextManager.addLocator(LOCATOR.getName(), LOCATOR_PREFIX, ctx.getMappingContext());
        verify(mappingContext, times(1)).put(any(), locMappingCaptor.capture());
        Srv6LocatorMapping mapping = locMappingCaptor.getValue();

        Assert.assertEquals(mapping.getPrefix(), LOCATOR_PREFIX);
        Assert.assertEquals(mapping.getName(), LOCATOR.getName());
    }

    @Test
    public void containsLocatorTest() {
        LocatorContextManagerImpl contextManager = new LocatorContextManagerImpl(64);
        boolean containsLocator = contextManager.containsLocator(LOCATOR.getName(), ctx.getMappingContext());
        Assert.assertTrue(containsLocator);
    }


    @Test
    public void getLocatorTest() {
        LocatorContextManagerImpl contextManager = new LocatorContextManagerImpl(64);
        Ipv6Prefix locator = contextManager.getLocator(LOCATOR.getName(), ctx.getMappingContext());
        Assert.assertEquals(LOCATOR_PREFIX, locator);
    }

    @Test
    public void removeLocatorTest() {
        MappingContext mappingContext = ctx.getMappingContext();
        LocatorContextManagerImpl contextManager = new LocatorContextManagerImpl(64);
        contextManager.removeLocator(LOCATOR.getName(), mappingContext);
        verify(mappingContext, times(1)).delete(iidCaptor.capture());
        Assert.assertEquals(LOCATOR.getName(), iidCaptor.getValue().firstKeyOf(Srv6LocatorMapping.class).getName());
    }
}
