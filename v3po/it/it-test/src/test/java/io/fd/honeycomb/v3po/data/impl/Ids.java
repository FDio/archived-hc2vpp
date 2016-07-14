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

package io.fd.honeycomb.v3po.data.impl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ComplexAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.ContainerWithList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.SimpleContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.choice.choice.c3.C3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.ListInContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.ContainerInList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.container.with.list.list.in.container.container.in.list.NestedList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.simple.container.ComplexAugmentContainer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hc.test.rev150105.some.attributes.ContainerFromGrouping;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Instance identifiers referencing all complex nodes within IT test-model.
 */
interface Ids {

    // Simple container
    // ORDER = 3
    InstanceIdentifier<SimpleContainer> SIMPLE_CONTAINER_ID = InstanceIdentifier.create(SimpleContainer.class);
    // 2
    InstanceIdentifier<SimpleAugment> SIMPLE_AUGMENT_ID = SIMPLE_CONTAINER_ID.augmentation(SimpleAugment.class);
    // UNORDERED
    InstanceIdentifier<ComplexAugment> COMPLEX_AUGMENT_ID = SIMPLE_CONTAINER_ID.augmentation(ComplexAugment.class);
    // 1
    InstanceIdentifier<ComplexAugmentContainer> COMPLEX_AUGMENT_CONTAINER_ID = COMPLEX_AUGMENT_ID.child(ComplexAugmentContainer.class);
    // Container with list
    // 9
    InstanceIdentifier<ContainerWithList> CONTAINER_WITH_LIST_ID = InstanceIdentifier.create(ContainerWithList.class);
    // 7
    InstanceIdentifier<ListInContainer> LIST_IN_CONTAINER_ID = CONTAINER_WITH_LIST_ID.child(ListInContainer.class);
    // 8
    InstanceIdentifier<ContainerInList> CONTAINER_IN_LIST_ID = LIST_IN_CONTAINER_ID.child(ContainerInList.class);
    // 6
    InstanceIdentifier<NestedList> NESTED_LIST_ID = CONTAINER_IN_LIST_ID.child(NestedList.class);
    // Container with choice
    // 4
    InstanceIdentifier<ContainerWithChoice> CONTAINER_WITH_CHOICE_ID = InstanceIdentifier.create(ContainerWithChoice.class);
    // 2
    InstanceIdentifier<C3> C3_ID = CONTAINER_WITH_CHOICE_ID.child(C3.class);
    // 5
    InstanceIdentifier<ContainerFromGrouping> CONTAINER_FROM_GROUPING_ID = CONTAINER_WITH_CHOICE_ID.child(ContainerFromGrouping.class);
}
