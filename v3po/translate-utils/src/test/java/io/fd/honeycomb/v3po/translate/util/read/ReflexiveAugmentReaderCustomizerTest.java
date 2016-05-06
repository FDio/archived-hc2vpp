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

package io.fd.honeycomb.v3po.translate.util.read;

import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.v3po.rev150105.VppInterfaceStateAugmentationBuilder;

public class ReflexiveAugmentReaderCustomizerTest {

    private ReflexiveAugmentReaderCustomizer<VppInterfaceStateAugmentation, VppInterfaceStateAugmentationBuilder>
        vppIfcStateAugmentCustomizer;

    @Before
    public void setUp() throws Exception {
        vppIfcStateAugmentCustomizer =
            new ReflexiveAugmentReaderCustomizer<>(VppInterfaceStateAugmentationBuilder.class,
                VppInterfaceStateAugmentation.class);
    }

    @Test
    public void testAddAugment() throws Exception {
        final InterfaceBuilder parentBuilder = new InterfaceBuilder();
        final VppInterfaceStateAugmentation augmentation = vppIfcStateAugmentCustomizer.getBuilder(null).build();
        vppIfcStateAugmentCustomizer.merge(parentBuilder, augmentation);
        assertSame(augmentation, parentBuilder.getAugmentation(VppInterfaceStateAugmentation.class));
    }
}